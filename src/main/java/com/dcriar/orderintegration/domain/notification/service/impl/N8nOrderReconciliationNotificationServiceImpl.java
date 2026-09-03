package com.dcriar.orderintegration.domain.notification.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.entity.NotificationOutboxEvent;
import com.dcriar.orderintegration.domain.notification.service.OrderReconciliationNotificationService;
import com.dcriar.orderintegration.domain.notification.model.NotificationOutboxStatus;
import com.dcriar.orderintegration.domain.notification.repository.NotificationOutboxRepository;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementação do serviço de disparo de notificações de conciliação financeira via Webhook para o n8n.
 * <p>
 * Utiliza o cliente HTTP reativo e moderno do Spring Boot ({@link RestClient}) com isolamento de falhas
 * e timeouts estritos (5 segundos), garantindo que instabilidades na rede de notificações não afetem a integridade
 * transacional do banco de dados ou bloqueiem as threads do worker de agendamento.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class N8nOrderReconciliationNotificationServiceImpl implements OrderReconciliationNotificationService {

    private final OrderIntegrationProperties properties;
    private final NotificationOutboxRepository outboxRepository;
    private final RestClient restClient;

    public N8nOrderReconciliationNotificationServiceImpl(
            OrderIntegrationProperties properties,
            NotificationOutboxRepository outboxRepository
    ) {
        this.properties = properties;
        this.outboxRepository = outboxRepository;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Transactional
    public void notifyReconciliationCompleted(OrderMaster order, BigDecimal subtotal, BigDecimal escrowAmount) {
        if (order == null) {
            log.warn("Tentativa de disparo de notificação de conciliação com pedido nulo.");
            return;
        }

        String webhookUrl = (properties != null && properties.notification() != null)
                ? properties.notification().n8nReconciliationWebhookUrl()
                : null;

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("URL de webhook de notificação de conciliação n8n não configurada. Disparo ignorado.");
            return;
        }

        Map<String, Object> audit = order.getMetadata() != null
                ? asObjectMap(order.getMetadata().get("auditoria_financeira"))
                : Map.of();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", order.getPlatform());
        payload.put("order_sn", order.getOrderSn());
        payload.put("shop_id", order.getShopId());
        payload.put("subtotal", subtotal != null ? subtotal : BigDecimal.ZERO);
        payload.put("escrow_amount", escrowAmount != null ? escrowAmount : BigDecimal.ZERO);
        payload.put("financial_audit_status", order.getFinancialAuditStatus());
        payload.put("auditoria_financeira", audit);
        payload.put("divergencia_financeira", asObjectMap(audit.get("divergencia_financeira")));

        NotificationOutboxEvent event = NotificationOutboxEvent.pending(
                "RECONCILIATION_COMPLETED",
                "ORDER",
                order.getPlatform() + ":" + order.getOrderSn(),
                payload
        );
        outboxRepository.save(event);
        log.info("Notificação de conciliação registrada na Outbox para o pedido '{}'.", order.getOrderSn());
    }

    /**
     * Entrega um lote de notificações pendentes ao n8n e registra o resultado de cada tentativa.
     *
     * @return quantidade de notificações entregues com sucesso
     */
    @Transactional
    public int dispatchPendingNotifications() {
        String webhookUrl = (properties != null && properties.notification() != null)
                ? properties.notification().n8nReconciliationWebhookUrl()
                : null;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("URL de webhook de notificação n8n não configurada. Outbox não processada.");
            return 0;
        }

        List<NotificationOutboxEvent> events = outboxRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        NotificationOutboxStatus.PENDING,
                        OffsetDateTime.now()
                );
        int sentCount = 0;
        for (NotificationOutboxEvent event : events) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(event.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                event.markSent();
                sentCount++;
            } catch (RestClientException exception) {
                event.registerFailure(exception.getMessage());
                log.warn("Falha ao entregar evento da Outbox '{}' ao n8n: {}",
                        event.getId(), exception.getMessage());
            }
            outboxRepository.save(event);
        }
        return sentCount;
    }

    private Map<String, Object> asObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, entryValue) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, entryValue);
            }
        });
        return result;
    }
}
