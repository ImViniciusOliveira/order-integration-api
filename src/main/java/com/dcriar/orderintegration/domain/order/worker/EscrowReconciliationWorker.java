package com.dcriar.orderintegration.domain.order.worker;

import com.dcriar.orderintegration.domain.order.service.EscrowReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker em segundo plano responsável pelo agendamento periódico da conciliação financeira de Escrow.
 * <p>
 * Acorda periodicamente no intervalo configurado (por padrão a cada 60 segundos),
 * consulta a fila de atraso do Redis (ZSet) e dispara o processamento dos pedidos
 * cujo tempo de espera contábil já foi atingido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscrowReconciliationWorker {

    private final EscrowReconciliationService reconciliationService;

    /**
     * Executa ciclicamente o job de conciliação de pedidos pendentes na fila do Redis.
     * O intervalo de espera entre a conclusão de uma execução e o início da próxima
     * é determinado pela propriedade {@code order-integration.escrow.worker-interval-ms}.
     */
    @Scheduled(fixedDelayString = "${order-integration.escrow.worker-interval-ms:60000}")
    public void executeReconciliationJob() {
        log.debug("Worker de conciliação de Escrow iniciado.");
        try {
            int reconciledCount = reconciliationService.reconcilePendingOrders();
            if (reconciledCount > 0) {
                log.info("Execução do worker finalizada: {} pedido(s) conciliado(s).", reconciledCount);
            }
        } catch (Exception e) {
            log.error("Falha durante a execução do worker de conciliação de Escrow: {}", e.getMessage(), e);
        }
    }
}
