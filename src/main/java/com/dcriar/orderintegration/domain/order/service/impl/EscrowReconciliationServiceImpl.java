package com.dcriar.orderintegration.domain.order.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.EscrowReconciliationService;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementação do serviço de conciliação financeira de Escrow / Settlement pós-venda.
 * <p>
 * Processa a fila com delay no Redis (ZSet), verifica os valores contábeis definitivos,
 * atualiza as entidades mestres de domínio rico e efetua o reagendamento inteligente (retry)
 * caso o marketplace ainda não tenha liberado o extrato fechado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowReconciliationServiceImpl implements EscrowReconciliationService {

    private final OrderMasterRepository orderMasterRepository;
    private final EscrowDelayQueueService delayQueueService;
    private final OrderIntegrationProperties properties;

    @Override
    public int reconcilePendingOrders() {
        int batchSize = (properties != null && properties.escrow() != null)
                ? properties.escrow().batchSize()
                : 50;

        Set<String> readyOrders = delayQueueService.pollReadyOrders(batchSize);
        if (readyOrders.isEmpty()) {
            log.debug("Nenhum pedido pendente de conciliação na fila do Redis.");
            return 0;
        }

        log.info("Iniciando processamento de conciliação de Escrow para {} pedido(s) resgatado(s) da fila.", readyOrders.size());
        int reconciledCount = 0;

        for (String member : readyOrders) {
            String[] parts = member.split(":", 2);
            if (parts.length != 2) {
                log.warn("Identificador de fila inválido encontrado no Redis: '{}'", member);
                continue;
            }

            String platform = parts[0];
            String orderSn = parts[1];

            try {
                boolean success = reconcileOrder(platform, orderSn);
                if (success) {
                    reconciledCount++;
                }
            } catch (Exception e) {
                log.error("Erro inesperado ao conciliar pedido '{}' ({}) da fila de Escrow: {}", orderSn, platform, e.getMessage(), e);
            }
        }

        log.info("Lote de conciliação finalizado: {}/{} pedido(s) reconciliado(s) com sucesso.", reconciledCount, readyOrders.size());
        return reconciledCount;
    }

    @Override
    @Transactional
    public boolean reconcileOrder(String platform, String orderSn) {
        if (platform == null || orderSn == null) {
            log.warn("Tentativa de conciliação com parâmetros nulos: platform={}, orderSn={}", platform, orderSn);
            return false;
        }

        String normalizedPlatform = platform.trim().toUpperCase();
        String normalizedOrderSn = orderSn.trim();

        Optional<OrderMaster> orderOptional = orderMasterRepository.findByPlatformAndOrderSn(normalizedPlatform, normalizedOrderSn);
        if (orderOptional.isEmpty()) {
            log.warn("Pedido '{}:{}' não foi encontrado no banco de dados. Removendo da fila do Redis.", normalizedPlatform, normalizedOrderSn);
            delayQueueService.remove(normalizedPlatform, normalizedOrderSn);
            return false;
        }

        OrderMaster order = orderOptional.get();

        // Se o pedido já foi conciliado previamente, apenas removemos da fila
        if (order.isReconciled()) {
            log.info("Pedido '{}:{}' já se encontra conciliado. Removendo da fila do Redis.", normalizedPlatform, normalizedOrderSn);
            delayQueueService.remove(normalizedPlatform, normalizedOrderSn);
            return true;
        }

        // Extração dos dados financeiros de liquidação do pedido
        EscrowSettlementData settlementData = extractSettlementData(order);

        if (settlementData.isAvailable()) {
            // Cenário A: Extrato contábil liberado com sucesso
            order.conciliarEscrow(settlementData.escrowAmount(), settlementData.shippingFeeBorneBySeller());
            orderMasterRepository.save(order);
            delayQueueService.remove(normalizedPlatform, normalizedOrderSn);

            log.info("Pedido '{}:{}' conciliado com sucesso: repasse líquido={}, frete cobrado do vendedor={}",
                    normalizedPlatform, normalizedOrderSn, settlementData.escrowAmount(), settlementData.shippingFeeBorneBySeller());
            return true;
        } else {
            // Cenário B: Extrato contábil ainda não liberado pela plataforma -> Reagendamento com retry
            int retryDelayMinutes = (properties != null && properties.escrow() != null)
                    ? properties.escrow().retryDelayMinutes()
                    : 30;

            delayQueueService.scheduleReconciliation(normalizedPlatform, normalizedOrderSn, Duration.ofMinutes(retryDelayMinutes));
            log.info("Extrato de Escrow ainda não liberado para o pedido '{}:{}'. Reagendado no Redis para nova tentativa em {} minutos.",
                    normalizedPlatform, normalizedOrderSn, retryDelayMinutes);
            return false;
        }
    }

    /**
     * Extrai os dados de liquidação contábil (Escrow) a partir do estado e metadados do pedido.
     *
     * @param order pedido mestre de domínio
     * @return dados de liquidação contendo o valor líquido e o frete cobrado
     */
    private EscrowSettlementData extractSettlementData(OrderMaster order) {
        Map<String, Object> metadata = order.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            // Se já tiver valores pré-carregados no próprio registro
            if (order.getEscrowAmount() != null && order.getEscrowAmount().compareTo(BigDecimal.ZERO) > 0) {
                return new EscrowSettlementData(order.getEscrowAmount(), order.getShippingFeeBorneBySeller(), true);
            }
            return new EscrowSettlementData(null, null, false);
        }

        BigDecimal escrowAmount = extractBigDecimalFromMap(metadata, "escrow_amount", "escrowAmount", "settlement_amount", "payout_amount");
        BigDecimal sellerShippingFee = extractBigDecimalFromMap(metadata, "shipping_fee_borne_by_seller", "shippingFeeBorneBySeller", "actual_shipping_fee");

        if (escrowAmount != null && escrowAmount.compareTo(BigDecimal.ZERO) > 0) {
            return new EscrowSettlementData(escrowAmount, sellerShippingFee != null ? sellerShippingFee : BigDecimal.ZERO, true);
        }

        if (order.getEscrowAmount() != null && order.getEscrowAmount().compareTo(BigDecimal.ZERO) > 0) {
            return new EscrowSettlementData(order.getEscrowAmount(), order.getShippingFeeBorneBySeller(), true);
        }

        return new EscrowSettlementData(null, null, false);
    }

    private BigDecimal extractBigDecimalFromMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return new BigDecimal(val.toString().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Record interno para transporte imutável dos valores contábeis extraídos de liquidação.
     */
    private record EscrowSettlementData(
            BigDecimal escrowAmount,
            BigDecimal shippingFeeBorneBySeller,
            boolean isAvailable
    ) {
    }
}
