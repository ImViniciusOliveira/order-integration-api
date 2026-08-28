package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Contrato de serviço para consulta paginada, busca e conciliação de pedidos mestre.
 */
public interface OrderMasterService {

    /**
     * Realiza a consulta paginada de pedidos aplicando filtros dinâmicos via Specifications.
     *
     * @param criteria critérios de filtro informados pelo usuário
     * @param pageable parâmetros de paginação e ordenação
     * @return página contendo as entidades {@link OrderMaster} correspondentes
     */
    Page<OrderMaster> searchOrders(OrderFilterCriteria criteria, Pageable pageable);

    /**
     * Localiza um pedido específico através da plataforma e número único do pedido.
     *
     * @param platform a plataforma de origem (ex: "SHOPEE")
     * @param orderSn  o número do pedido
     * @return um {@link Optional} com o pedido correspondente
     */
    Optional<OrderMaster> findByPlatformAndOrderSn(String platform, String orderSn);

    /**
     * Executa a conciliação financeira de Escrow de um pedido fechado.
     *
     * @param platform                 plataforma de origem
     * @param orderSn                  número do pedido
     * @param escrowAmount             valor líquido repassado
     * @param shippingFeeBorneBySeller custo real de frete suportado pelo vendedor
     * @return o pedido atualizado e marcado como conciliado
     */
    OrderMaster reconcileEscrow(String platform, String orderSn, BigDecimal escrowAmount, BigDecimal shippingFeeBorneBySeller);
}
