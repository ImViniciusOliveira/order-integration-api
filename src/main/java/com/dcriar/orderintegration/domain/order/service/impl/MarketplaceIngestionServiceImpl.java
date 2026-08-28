package com.dcriar.orderintegration.domain.order.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.repository.MarketplaceChannelRepository;
import com.dcriar.orderintegration.domain.order.entity.MarketplaceRawEvent;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderProcessingResult;
import com.dcriar.orderintegration.domain.order.processor.MarketplaceOrderProcessor;
import com.dcriar.orderintegration.domain.order.repository.MarketplaceRawEventRepository;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.MarketplaceIngestionService;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementação do serviço de ingestão e processamento de webhooks e eventos de marketplaces.
 * <p>
 * Aplica rigorosamente o pipeline de integridade:
 * <ol>
 *   <li>Validação de canal ativo no banco de dados.</li>
 *   <li>Persistência imutável no Event Store ({@link MarketplaceRawEvent}).</li>
 *   <li>Delegação da extração de dados para a Strategy correspondente ({@link MarketplaceOrderProcessor}).</li>
 *   <li>Atualização do modelo mestre de domínio rico ({@link OrderMaster}).</li>
 *   <li>Persistência no PostgreSQL primeiro; enfileiramento no Redis (ZSet Delay Queue) apenas após confirmação.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceIngestionServiceImpl implements MarketplaceIngestionService {

    private final MarketplaceChannelRepository channelRepository;
    private final MarketplaceRawEventRepository rawEventRepository;
    private final OrderMasterRepository orderMasterRepository;
    private final EscrowDelayQueueService delayQueueService;
    private final OrderIntegrationProperties properties;
    private final List<MarketplaceOrderProcessor> processors;

    @Override
    @Transactional
    public OrderMaster ingestEvent(String platform, String shopId, Map<String, Object> payload) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("O código da plataforma é obrigatório para ingestão");
        }

        String normalizedPlatform = platform.trim().toUpperCase();

        // 1. Validar se o canal está cadastrado e ativo no banco
        MarketplaceChannel channel = channelRepository.findByCodeAndActiveTrue(normalizedPlatform)
                .orElseThrow(() -> new IllegalStateException("Canal de marketplace '" + normalizedPlatform + "' não está ativo ou não foi cadastrado"));

        // 2. Localizar o processador (Strategy) correspondente
        MarketplaceOrderProcessor processor = processors.stream()
                .filter(p -> p.supports(normalizedPlatform))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Nenhum processador registrado para a plataforma: " + normalizedPlatform));

        // 3. Extrair dados padronizados através do processador
        OrderProcessingResult result = processor.process(shopId, payload);

        // 4. Salvar evento no Event Store imutável para auditoria
        MarketplaceRawEvent rawEvent = MarketplaceRawEvent.criarEvento(
                normalizedPlatform,
                result.shopId(),
                result.orderSn(),
                result.status(),
                payload
        );
        rawEventRepository.save(rawEvent);
        log.info("Evento bruto de auditoria gravado no Event Store: id={}, plataforma={}, pedido={}",
                rawEvent.getId(), normalizedPlatform, result.orderSn());

        // 5. Localizar ou instanciar o OrderMaster
        OrderMaster order = orderMasterRepository.findByPlatformAndOrderSn(normalizedPlatform, result.orderSn())
                .orElseGet(() -> OrderMaster.builder()
                        .platform(normalizedPlatform)
                        .shopId(result.shopId())
                        .orderSn(result.orderSn())
                        .reconciled(false)
                        .build());

        // 6. Aplicar regras de domínio rico
        if (result.shopId() != null && !result.shopId().isBlank()) {
            order.setShopId(result.shopId());
        }

        if (result.estimatedShippingFee() != null) {
            order.provisionarEstimativa(result.status(), result.estimatedShippingFee(), result.metadata());
        } else {
            if (result.metadata() != null) {
                order.setMetadata(result.metadata());
            }
            if (result.status() != null && !result.status().isBlank()) {
                order.atualizarStatus(result.status());
            }
        }

        if (result.trackingNo() != null && !result.trackingNo().isBlank()) {
            order.atualizarRastreio(result.trackingNo());
        }

        // 7. ORDEM OBRIGATÓRIA - PASSO 1: Salvar primeiro no PostgreSQL (Fonte Primária)
        OrderMaster savedOrder = orderMasterRepository.save(order);
        log.info("Pedido mestre unificado persistido no PostgreSQL com sucesso: id={}, status={}, reconciliado={}",
                savedOrder.getId(), savedOrder.getStatus(), savedOrder.isReconciled());

        // 8. ORDEM OBRIGATÓRIA - PASSO 2: Agendar no Redis apenas APÓS sucesso no banco se status for COMPLETED
        if ("COMPLETED".equalsIgnoreCase(savedOrder.getStatus()) && !savedOrder.isReconciled()) {
            Duration delay = resolveEscrowDelay();
            delayQueueService.scheduleReconciliation(savedOrder.getPlatform(), savedOrder.getOrderSn(), delay);
        }

        return savedOrder;
    }

    private Duration resolveEscrowDelay() {
        if (properties != null && properties.escrow() != null && properties.escrow().delayMinutes() > 0) {
            return Duration.ofMinutes(properties.escrow().delayMinutes());
        }
        return Duration.ofHours(2);
    }
}
