package com.dcriar.orderintegration.domain.order.service.impl;

import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.OrderMasterService;
import com.dcriar.orderintegration.domain.order.specification.OrderMasterSpecifications;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
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
 * Implementação do serviço de domínio responsável pela gestão, consulta paginada e conciliação de pedidos mestre {@link OrderMaster}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderMasterServiceImpl implements OrderMasterService {

    private final OrderMasterRepository orderMasterRepository;

    @Override
    public OrderMaster findById(Long id) {
        return orderMasterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));
    }

    @Override
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

    @Override
    public Optional<OrderMaster> findByPlatformAndOrderSn(String platform, String orderSn) {
        return orderMasterRepository.findByPlatformAndOrderSn(platform, orderSn);
    }

    @Override
    @Transactional
    public OrderMaster reconcileEscrow(String platform, String orderSn, BigDecimal escrowAmount, BigDecimal shippingFeeBorneBySeller) {
        OrderMaster order = orderMasterRepository.findByPlatformAndOrderSn(platform, orderSn)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado para conciliação: " + platform + ":" + orderSn));

        order.conciliarEscrow(escrowAmount, shippingFeeBorneBySeller);
        OrderMaster saved = orderMasterRepository.save(order);

        log.info("Conciliação de Escrow realizada com sucesso para o pedido {}:{}. Valor={}",
                platform, orderSn, escrowAmount);

        return saved;
    }
}
