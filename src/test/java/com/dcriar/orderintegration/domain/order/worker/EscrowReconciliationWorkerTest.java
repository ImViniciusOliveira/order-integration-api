package com.dcriar.orderintegration.domain.order.worker;

import com.dcriar.orderintegration.domain.order.service.EscrowReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar a execução do agendador periódico (EscrowReconciliationWorker).
 */
@ExtendWith(MockitoExtension.class)
class EscrowReconciliationWorkerTest {

    @Mock
    private EscrowReconciliationService reconciliationService;

    @InjectMocks
    private EscrowReconciliationWorker worker;

    @Test
    @DisplayName("Deve invocar o serviço de conciliação de pedidos pendentes ao disparar o job")
    void deveInvocarServicoDeConciliacao() {
        // Arrange
        when(reconciliationService.reconcilePendingOrders()).thenReturn(3);

        // Act
        worker.executeReconciliationJob();

        // Assert
        verify(reconciliationService).reconcilePendingOrders();
    }
}
