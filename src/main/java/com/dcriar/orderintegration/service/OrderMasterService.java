package com.dcriar.orderintegration.service;

import com.dcriar.orderintegration.domain.model.OrderMaster;
import com.dcriar.orderintegration.domain.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.specification.OrderMasterSpecifications;
import com.dcriar.orderintegration.service.dto.OrderFilterCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Serviço de domínio responsável pela gestão, consulta paginada e conciliação de pedidos mestre {@link OrderMaster}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMasterService {

    private final OrderMasterRepository orderMasterRepository;

    /**
     * Realiza a consulta paginada de pedidos aplicando filtros dinâmicos via Specifications.
     *
     * @param criteria critérios de filtro informados pelo usuário
     * @param pageable parâmetros de paginação e ordenação
     * @return página contendo as entidades {@link OrderMaster} correspondentes
     */
    @Transactional(readOnly = true)
    public Page<OrderMaster> searchOrders(OrderFilterCriteria criteria, Pageable pageable) {
        Specification<OrderMaster> spec = (root, query, cb) -> cb.conjunction();

        if (criteria != null) {
            spec = spec
                    .and(OrderMasterSpecifications.byPlatform(criteria.platform()))
                    .and(OrderMasterSpecifications.byShopId(criteria.shopId()))
                    .and(OrderMasterSpecifications.byStatus(criteria.status()))
                    .and(OrderMasterSpecifications.byReconciled(criteria.reconciled()))
                    .and(OrderMasterSpecifications.byOrderSn(criteria.orderSn()))
                    .and(OrderMasterSpecifications.byTrackingNo(criteria.trackingNo()))
                    .and(OrderMasterSpecifications.byCreatedAtBetween(criteria.startDate(), criteria.endDate()));
        }

        return orderMasterRepository.findAll(spec, pageable);
    }

    /**
     * Localiza um pedido específico através da plataforma e número único do pedido.
     *
     * @param platform a plataforma de origem (ex: "SHOPEE", "TIKTOK")
     * @param orderSn  o número do pedido
     * @return um {@link Optional} com o pedido correspondente
     */
    @Transactional(readOnly = true)
    public Optional<OrderMaster> findByPlatformAndOrderSn(String platform, String orderSn) {
        return orderMasterRepository.findByPlatformAndOrderSn(platform, orderSn);
    }

    /**
     * Executa a conciliação financeira de Escrow de um pedido fechado.
     *
     * @param platform                 plataforma de origem
     * @param orderSn                  número do pedido
     * @param escrowAmount             valor líquido repassado
     * @param shippingFeeBorneBySeller custo real de frete suportado pelo vendedor
     * @return o pedido atualizado e marcado como conciliado
     */
    @Transactional
    public OrderMaster reconcileEscrow(String platform, String orderSn, BigDecimal escrowAmount, BigDecimal shippingFeeBorneBySeller) {
        OrderMaster order = orderMasterRepository.findByPlatformAndOrderSn(platform, orderSn)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado para conciliação: " + platform + ":" + orderSn));

        order.conciliarEscrow(escrowAmount, shippingFeeBorneBySeller);
        OrderMaster saved = orderMasterRepository.save(order);

        log.info("Conciliação de Escrow realizada com sucesso para o pedido {}:{}. Valor={}",
                platform, orderSn, escrowAmount);

        return saved;
    }
}
