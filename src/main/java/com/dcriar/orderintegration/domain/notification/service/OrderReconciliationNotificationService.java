package com.dcriar.orderintegration.domain.notification.service;

import com.dcriar.orderintegration.domain.order.entity.OrderMaster;

import java.math.BigDecimal;

/**
 * Contrato de serviço para disparo de notificações externas de conciliação de pedidos.
 */
public interface OrderReconciliationNotificationService {

    /**
     * Notifica o gateway de automação (n8n) sobre o fechamento contábil e liquidação do pedido.
     *
     * @param order        entidade mestre do pedido conciliado
     * @param subtotal     valor bruto total de venda dos itens do pedido
     * @param escrowAmount valor líquido real repassado pelo marketplace
     */
    void notifyReconciliationCompleted(OrderMaster order, BigDecimal subtotal, BigDecimal escrowAmount);
}
