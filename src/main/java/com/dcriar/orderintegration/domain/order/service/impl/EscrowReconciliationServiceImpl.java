package com.dcriar.orderintegration.domain.order.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.service.OrderReconciliationNotificationService;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.MarketplaceFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.mapper.FeeCalculationMapper;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeAuditStatus;
import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import com.dcriar.orderintegration.domain.marketplace.common.service.MarketplaceSettlementClient;
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
import java.util.LinkedHashMap;
import java.util.List;
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
@Transactional(readOnly = true)
public class EscrowReconciliationServiceImpl implements EscrowReconciliationService {

    private final OrderMasterRepository orderMasterRepository;
    private final EscrowDelayQueueService delayQueueService;
    private final OrderIntegrationProperties properties;
    private final List<MarketplaceFeeCalculator> feeCalculators;
    private final List<MarketplaceSettlementClient> settlementClients;
    private final FeeCalculationMapper feeCalculationMapper;
    private final OrderReconciliationNotificationService notificationService;

    @Override
    @Transactional
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

        String normalizedPlatform = platform.toUpperCase();
        String normalizedOrderSn = orderSn;

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
            delayQueueService.clearRetry(normalizedPlatform, normalizedOrderSn);
            return true;
        }

        MarketplaceSettlement settlement = fetchSettlement(normalizedPlatform, order);
        if (settlement == null || settlement.status() == null) {
            throw new IllegalStateException("Client de settlement retornou resultado inválido para "
                    + normalizedPlatform + ":" + normalizedOrderSn);
        }

        if (settlement.status() != SettlementStatus.AVAILABLE) {
            handleUnavailableSettlement(normalizedPlatform, normalizedOrderSn, settlement.status());
            return false;
        }

        Map<String, Object> settlementMetadata = new LinkedHashMap<>(
                order.getMetadata() != null ? order.getMetadata() : Map.of()
        );
        settlementMetadata.put("settlement_financial_details", settlement.financialDetails());
        order.setMetadata(settlementMetadata);

        BigDecimal subtotalCalculated = BigDecimal.ZERO;

        // Cenário A: Extrato contábil liberado com sucesso -> Executar prova real matemática de taxas
        if (feeCalculators != null && !feeCalculators.isEmpty()) {
            Optional<MarketplaceFeeCalculator> calculatorOpt = feeCalculators.stream()
                    .filter(c -> c.supports(normalizedPlatform))
                    .findFirst();

            if (calculatorOpt.isPresent()) {
                FeeCalculationResult feeResult = calculatorOpt.get().calculate(
                        order,
                        settlement.netAmount(),
                        settlement.shippingFee()
                );
                subtotalCalculated = feeResult.subtotalItems();
                        settlementMetadata.put("auditoria_financeira", feeCalculationMapper.toMap(feeResult));
                        order.setMetadata(settlementMetadata);
                        if (feeResult.auditStatus() == FeeAuditStatus.INCOMPLETE) {
                            orderMasterRepository.save(order);
                            handleUnavailableSettlement(normalizedPlatform, normalizedOrderSn, SettlementStatus.PENDING);
                            return false;
                        }
                }
        }

        order.conciliarEscrow(settlement.netAmount(), settlement.shippingFee());
        OrderMaster savedOrder = orderMasterRepository.save(order);
        delayQueueService.remove(normalizedPlatform, normalizedOrderSn);
        delayQueueService.clearRetry(normalizedPlatform, normalizedOrderSn);

        log.info("Pedido '{}:{}' conciliado com sucesso: repasse líquido={}, frete cobrado do vendedor={}",
                normalizedPlatform, normalizedOrderSn, settlement.netAmount(), settlement.shippingFee());

        // Disparo de notificação externa (n8n / Telegram)
        if (notificationService != null) {
            notificationService.notifyReconciliationCompleted(savedOrder, subtotalCalculated, settlement.netAmount());
        }

        return true;
    }

    private void handleUnavailableSettlement(String platform, String orderSn, SettlementStatus status) {
        if (status == SettlementStatus.PERMANENT_ERROR) {
            delayQueueService.moveToDeadLetterQueue(platform, orderSn);
            log.error("Settlement do pedido '{}:{}' falhou definitivamente. Item removido da fila.",
                    platform, orderSn);
            return;
        }

        int maxRetries = (properties != null && properties.escrow() != null)
                ? properties.escrow().maxRetries()
                : 5;
        long retryCount = delayQueueService.incrementRetry(platform, orderSn);
        if (retryCount > maxRetries) {
            delayQueueService.moveToDeadLetterQueue(platform, orderSn);
            log.error("Pedido '{}:{}' excedeu o limite de {} tentativas e foi movido para a DLQ.",
                    platform, orderSn, maxRetries);
            return;
        }

        int retryDelayMinutes = (properties != null && properties.escrow() != null)
                ? properties.escrow().retryDelayMinutes()
                : 30;

        delayQueueService.scheduleReconciliation(platform, orderSn, Duration.ofMinutes(retryDelayMinutes));
        log.info("Settlement do pedido '{}:{}' está em estado {}. Reagendado para nova tentativa em {} minutos.",
                platform, orderSn, status, retryDelayMinutes);
    }

    private MarketplaceSettlement fetchSettlement(String platform, OrderMaster order) {
        if (settlementClients == null) {
            throw new IllegalStateException("Nenhum client de settlement foi configurado");
        }

        MarketplaceSettlementClient client = settlementClients.stream()
                .filter(candidate -> candidate.supports(platform))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum client de settlement suporta a plataforma " + platform
                ));

        return client.fetchSettlement(order.getShopId(), order.getOrderSn());
    }
}
