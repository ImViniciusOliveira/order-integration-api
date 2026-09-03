package com.dcriar.orderintegration.domain.notification.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.service.OrderReconciliationNotificationService;
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
    private final RestClient restClient;

    public N8nOrderReconciliationNotificationServiceImpl(OrderIntegrationProperties properties) {
        this.properties = properties;
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

        boolean hasDivergence = false;
        if (order.getMetadata() != null && order.getMetadata().containsKey("auditoria_financeira")) {
            Object auditoriaObj = order.getMetadata().get("auditoria_financeira");
            if (auditoriaObj instanceof Map<?, ?> auditoriaMap) {
                Object divObj = auditoriaMap.get("has_divergence");
                if (divObj != null) {
                    hasDivergence = Boolean.parseBoolean(divObj.toString());
                }
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", order.getPlatform());
        payload.put("order_sn", order.getOrderSn());
        payload.put("shop_id", order.getShopId());
        payload.put("subtotal", subtotal != null ? subtotal : BigDecimal.ZERO);
        payload.put("escrow_amount", escrowAmount != null ? escrowAmount : BigDecimal.ZERO);
        payload.put("has_divergence", hasDivergence);
        payload.put("auditoria_financeira", order.getMetadata() != null
                ? order.getMetadata().get("auditoria_financeira")
                : Map.of());

        try {
            log.info("Disparando webhook de conciliação finalizada para o n8n: url='{}', pedido='{}'", webhookUrl, order.getOrderSn());
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notificação de conciliação entregue com sucesso ao n8n para o pedido '{}'.", order.getOrderSn());
        } catch (RestClientException e) {
            log.warn("Falha ao entregar notificação de conciliação ao n8n para o pedido '{}': {}", order.getOrderSn(), e.getMessage());
        }
    }
}
