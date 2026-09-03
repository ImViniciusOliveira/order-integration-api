package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.service.OrderReconciliationNotificationService;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.MercadoLivreFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.shopee.calculator.ShopeeCpfFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.mapper.FeeCalculationMapper;
import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import com.dcriar.orderintegration.domain.marketplace.common.service.MarketplaceSettlementClient;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.impl.EscrowReconciliationServiceImpl;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Suite de testes unitários para a implementação do serviço {@link EscrowReconciliationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class EscrowReconciliationServiceTest {

    @Mock
    private OrderMasterRepository orderMasterRepository;

    @Mock
    private EscrowDelayQueueService delayQueueService;

    @Mock
    private OrderReconciliationNotificationService notificationService;

    @Mock
    private MarketplaceSettlementClient shopeeSettlementClient;

    @Mock
    private MarketplaceSettlementClient mercadoLivreSettlementClient;

    private EscrowReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("dcriar:orders:escrow_delay_queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("test-key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost:8081")),
                new OrderIntegrationProperties.NotificationProperties("http://n8n-order:5678/webhook/v1/notifications/reconciled"),
                new OrderIntegrationProperties.ShopeeProperties(
                        "https://partner.shopeemobile.com",
                        "/api/v2/payment/get_escrow_detail",
                        "/api/v2/auth/access_token/get"
                ),
                new OrderIntegrationProperties.MercadoLivreProperties(
                        "https://api.mercadolibre.com", "/orders", "/shipments", "/v1/payments", "/oauth/token"
                )
        );

        reconciliationService = new EscrowReconciliationServiceImpl(
                orderMasterRepository,
                delayQueueService,
                properties,
                List.of(new ShopeeCpfFeeCalculator(), new MercadoLivreFeeCalculator()),
                List.of(shopeeSettlementClient, mercadoLivreSettlementClient),
                new FeeCalculationMapper(),
                notificationService
        );

    }

    @Test
    @DisplayName("Deve conciliar pedidos pendentes com sucesso resgatados da fila e gravar auditoria financeira")
    void deveConciliarPedidosPendentesComSucesso() {
        // Arrange
        Map<String, Object> item = Map.of(
                "item_id", 123L,
                "item_name", "Produto Teste",
                "model_discounted_price", "100.00",
                "model_quantity_purchased", 1
        );

        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("240828ABC")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of(
                        "escrow_amount", 76.00,
                        "shipping_fee_borne_by_seller", 0.00,
                        "item_list", List.of(item)
                ))
                .build();

        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("SHOPEE:240828ABC"));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC")).thenReturn(Optional.of(order));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));
        when(shopeeSettlementClient.supports("SHOPEE")).thenReturn(true);
        when(shopeeSettlementClient.fetchSettlement("123", "240828ABC")).thenReturn(
                availableSettlement("SHOPEE", "240828ABC", "123", "76.00", "0.00")
        );

        // Act
        int reconciledCount = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(reconciledCount).isEqualTo(1);
        assertThat(order.isReconciled()).isTrue();
        assertThat(order.getEscrowAmount()).isEqualByComparingTo(new BigDecimal("76.00"));
        assertThat(order.getShippingFeeBorneBySeller()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getMetadata()).containsKey("auditoria_financeira");

        @SuppressWarnings("unchecked")
        Map<String, Object> auditoria = (Map<String, Object>) order.getMetadata().get("auditoria_financeira");
        assertThat(auditoria).isNotNull();
        assertThat(auditoria.get("versao_regra")).isEqualTo("SHOPEE_OFFICIAL_ORDER_INCOME");
        assertThat(auditoria.get("has_divergence")).isEqualTo(false);
        assertThat(order.getMetadata()).containsKey("snapshot_financeiro");

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot =
                (Map<String, Object>) order.getMetadata().get("snapshot_financeiro");
        assertThat(snapshot.get("plataforma")).isEqualTo("SHOPEE");
        assertThat(snapshot.get("valor_liquido")).isEqualTo(new BigDecimal("76.00"));
        assertThat(snapshot).containsKey("detalhes_externos");

        verify(orderMasterRepository).save(order);
        verify(delayQueueService).remove("SHOPEE", "240828ABC");
        verify(notificationService).notifyReconciliationCompleted(
                eq(order),
                argThat(v -> v != null && v.compareTo(new BigDecimal("100.00")) == 0),
                argThat(v -> v != null && v.compareTo(new BigDecimal("76.00")) == 0)
        );
        verify(shopeeSettlementClient).fetchSettlement("123", "240828ABC");
    }

    @Test
    @DisplayName("Deve conciliar pedidos pendentes do Mercado Livre com sucesso resgatados da fila")
    void deveConciliarPedidosPendentesMercadoLivreComSucesso() {
        // Arrange
        Map<String, Object> item = Map.of(
                "item_name", "Glitter Roxo",
                "unit_price", 20.00,
                "quantity", 1
        );

        OrderMaster order = OrderMaster.builder()
                .id(2L)
                .platform("MERCADOLIVRE")
                .shopId("3644237792")
                .orderSn("2000018236707690")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of(
                        "sale_fee", 2.30,
                        "escrow_amount", 17.70,
                        "shipping_fee_borne_by_seller", 0.00,
                        "items", List.of(item)
                ))
                .build();

        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("MERCADOLIVRE:2000018236707690"));
        when(orderMasterRepository.findByPlatformAndOrderSn("MERCADOLIVRE", "2000018236707690")).thenReturn(Optional.of(order));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));
        when(shopeeSettlementClient.supports("MERCADOLIVRE")).thenReturn(false);
        when(mercadoLivreSettlementClient.supports("MERCADOLIVRE")).thenReturn(true);
        when(mercadoLivreSettlementClient.fetchSettlement("3644237792", "2000018236707690")).thenReturn(
                availableSettlement("MERCADOLIVRE", "2000018236707690", "3644237792", "17.70", "0.00")
        );

        // Act
        int reconciledCount = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(reconciledCount).isEqualTo(1);
        assertThat(order.isReconciled()).isTrue();
        assertThat(order.getEscrowAmount()).isEqualByComparingTo(new BigDecimal("17.70"));
        assertThat(order.getShippingFeeBorneBySeller()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getMetadata()).containsKey("auditoria_financeira");

        @SuppressWarnings("unchecked")
        Map<String, Object> auditoria = (Map<String, Object>) order.getMetadata().get("auditoria_financeira");
        assertThat(auditoria).isNotNull();
        assertThat(auditoria.get("versao_regra")).isEqualTo("MERCADOLIVRE_BR_2026");
        assertThat(auditoria.get("has_divergence")).isEqualTo(false);

        verify(orderMasterRepository).save(order);
        verify(delayQueueService).remove("MERCADOLIVRE", "2000018236707690");
        verify(notificationService).notifyReconciliationCompleted(
                eq(order),
                argThat(v -> v != null && v.compareTo(new BigDecimal("20.00")) == 0),
                argThat(v -> v != null && v.compareTo(new BigDecimal("17.70")) == 0)
        );
        verify(mercadoLivreSettlementClient).fetchSettlement("3644237792", "2000018236707690");
    }

    @Test
    @DisplayName("Deve reagendar conciliação com retry quando pedido estiver pendente de extrato")
    void deveReagendarConciliacaoQuandoPendente() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(2L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("PENDING_SN")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of()) // sem dados de escrow
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "PENDING_SN")).thenReturn(Optional.of(order));
        when(shopeeSettlementClient.supports("SHOPEE")).thenReturn(true);
        when(shopeeSettlementClient.fetchSettlement("123", "PENDING_SN")).thenReturn(
                new MarketplaceSettlement(
                        SettlementStatus.PENDING, "SHOPEE", "PENDING_SN", "123",
                        null, null, null, null, null, null, Map.of(), null, "Pendente"
                )
        );

        // Act
        boolean result = reconciliationService.reconcileOrder("SHOPEE", "PENDING_SN");

        // Assert
        assertThat(result).isFalse();
        assertThat(order.isReconciled()).isFalse();
        verify(delayQueueService).scheduleReconciliation("SHOPEE", "PENDING_SN", Duration.ofMinutes(30));
        verify(orderMasterRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Deve remover da fila quando settlement falhar definitivamente")
    void deveRemoverDaFilaQuandoSettlementFalharDefinitivamente() {
        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("PERMANENT_ERROR_SN")
                .reconciled(false)
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "PERMANENT_ERROR_SN"))
                .thenReturn(Optional.of(order));
        when(shopeeSettlementClient.supports("SHOPEE")).thenReturn(true);
        when(shopeeSettlementClient.fetchSettlement("123", "PERMANENT_ERROR_SN")).thenReturn(
                new MarketplaceSettlement(
                        SettlementStatus.PERMANENT_ERROR, "SHOPEE", "PERMANENT_ERROR_SN", "123",
                        null, null, null, null, null, null, Map.of(), null, "Pedido inválido"
                )
        );

        boolean result = reconciliationService.reconcileOrder("SHOPEE", "PERMANENT_ERROR_SN");

        assertThat(result).isFalse();
        verify(delayQueueService).moveToDeadLetterQueue("SHOPEE", "PERMANENT_ERROR_SN");
        verify(delayQueueService, never()).scheduleReconciliation(anyString(), anyString(), any());
        verify(orderMasterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve apenas remover da fila se pedido já estiver conciliado")
    void deveRemoverDaFilaSeJaConciliado() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(3L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("ALREADY_RECONCILED")
                .status("COMPLETED")
                .reconciled(true)
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "ALREADY_RECONCILED")).thenReturn(Optional.of(order));

        // Act
        boolean result = reconciliationService.reconcileOrder("SHOPEE", "ALREADY_RECONCILED");

        // Assert
        assertThat(result).isTrue();
        verify(delayQueueService).remove("SHOPEE", "ALREADY_RECONCILED");
        verify(orderMasterRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Deve remover da fila se pedido não for encontrado no banco de dados")
    void deveRemoverDaFilaSeNaoEncontradoNoBanco() {
        // Arrange
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "NOT_FOUND")).thenReturn(Optional.empty());

        // Act
        boolean result = reconciliationService.reconcileOrder("SHOPEE", "NOT_FOUND");

        // Assert
        assertThat(result).isFalse();
        verify(delayQueueService).remove("SHOPEE", "NOT_FOUND");
        verify(orderMasterRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Deve retornar zero se a fila do Redis estiver vazia")
    void deveRetornarZeroSeFilaVazia() {
        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of());

        int count = reconciliationService.reconcilePendingOrders();

        assertThat(count).isZero();
        verifyNoInteractions(orderMasterRepository);
    }

    private MarketplaceSettlement availableSettlement(
            String platform,
            String orderId,
            String accountId,
            String netAmount,
            String shippingFee
    ) {
        Map<String, Object> financialDetails = platform.equals("SHOPEE")
                ? Map.of(
                        "order_income", Map.of(
                                "commission_fee", "24.00",
                                "service_fee", "0.00",
                                "seller_transaction_fee", "0.00",
                                "income_details", Map.of("actual_shipping_fee", shippingFee)
                        )
                )
                : Map.of();
        return new MarketplaceSettlement(
                SettlementStatus.AVAILABLE,
                platform,
                orderId,
                accountId,
                null,
                new BigDecimal(netAmount),
                null,
                null,
                new BigDecimal(shippingFee),
                null,
                financialDetails,
                null,
                null
        );
    }
}
