package com.dcriar.orderintegration.domain.notification.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.entity.NotificationOutboxEvent;
import com.dcriar.orderintegration.domain.notification.model.NotificationOutboxStatus;
import com.dcriar.orderintegration.domain.notification.repository.NotificationOutboxRepository;
import com.dcriar.orderintegration.domain.notification.service.impl.N8nOrderReconciliationNotificationServiceImpl;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do registro e despacho de eventos da Outbox de notificações.
 */
@ExtendWith(MockitoExtension.class)
class N8nOrderReconciliationNotificationServiceTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private N8nOrderReconciliationNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(restClientBuilder.requestFactory(any(ClientHttpRequestFactory.class)))
                .thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        service = new N8nOrderReconciliationNotificationServiceImpl(
                properties(), outboxRepository, restClientBuilder
        );
    }

    @Test
    @DisplayName("Deve registrar a notificação na Outbox em vez de chamar o n8n diretamente")
    void deveRegistrarNotificacaoNaOutbox() {
        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("OUTBOX-001")
                .metadata(Map.of("auditoria_financeira", Map.of("has_divergence", true)))
                .build();

        service.notifyReconciliationCompleted(order, new BigDecimal("100.00"), new BigDecimal("80.00"));

        ArgumentCaptor<NotificationOutboxEvent> captor =
                ArgumentCaptor.forClass(NotificationOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(captor.getValue().getAggregateId()).isEqualTo("SHOPEE:OUTBOX-001");
        assertThat(captor.getValue().getPayload())
                .containsEntry("escrow_amount", new BigDecimal("80.00"))
                .containsKey("divergencia_financeira");
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("Deve entregar evento pendente e marcar como enviado")
    void deveEntregarEventoPendente() {
        NotificationOutboxEvent event = NotificationOutboxEvent.pending(
                "RECONCILIATION_COMPLETED",
                "ORDER",
                "SHOPEE:OUTBOX-002",
                Map.of("order_sn", "OUTBOX-002")
        );
        when(outboxRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(NotificationOutboxStatus.PENDING), any(OffsetDateTime.class)
        )).thenReturn(List.of(event));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(anyMap());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        int sent = service.dispatchPendingNotifications();

        assertThat(sent).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
        verify(outboxRepository).save(event);
    }

    private OrderIntegrationProperties properties() {
        return new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost")),
                new OrderIntegrationProperties.NotificationProperties("http://n8n/webhook"),
                new OrderIntegrationProperties.ShopeeProperties("https://shopee", "/escrow", "/token"),
                new OrderIntegrationProperties.MercadoLivreProperties(
                        "https://mercadolivre", "/orders", "/shipments", "/payments", "/oauth"
                )
        );
    }
}
