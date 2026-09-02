package com.dcriar.orderintegration.domain.order.specification;

import com.dcriar.orderintegration.domain.common.PostgresSearchUtils;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Especificações dinâmicas (Specifications) baseadas na Criteria API do JPA
 * para consultas e filtros combináveis na entidade {@link OrderMaster}.
 */
public final class OrderMasterSpecifications {

    private OrderMasterSpecifications() {
    }

    /**
     * Filtra pedidos pelo código exato da plataforma (ex: "SHOPEE").
     *
     * @param platform código da plataforma
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byPlatform(String platform) {
        return (root, query, cb) -> {
            if (platform == null || platform.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("platform")), platform.toUpperCase());
        };
    }

    /**
     * Filtra pedidos pelo identificador da loja (shop_id).
     *
     * @param shopId identificador da loja
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byShopId(String shopId) {
        return (root, query, cb) -> {
            if (shopId == null || shopId.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("shopId"), shopId);
        };
    }

    /**
     * Filtra pedidos pelo status do ciclo de vida.
     *
     * @param status status a ser filtrado (ex: "READY_TO_SHIP", "COMPLETED")
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("status")), status.toUpperCase());
        };
    }

    /**
     * Filtra pedidos pelo estado de conciliação financeira de Escrow.
     *
     * @param reconciled indicador de conciliação concluída
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byReconciled(Boolean reconciled) {
        return (root, query, cb) -> {
            if (reconciled == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("reconciled"), reconciled);
        };
    }

    /**
     * Filtra pedidos por correspondência de texto no número do pedido (order_sn) com suporte a unaccent e lower.
     *
     * @param orderSn termo de busca
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byOrderSn(String orderSn) {
        return (root, query, cb) -> PostgresSearchUtils.containsNormalized(cb, root.get("orderSn"), orderSn);
    }

    /**
     * Filtra pedidos pelo código de rastreamento com busca textual insensível a acentos e maiúsculas.
     *
     * @param trackingNo código de rastreamento
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byTrackingNo(String trackingNo) {
        return (root, query, cb) -> PostgresSearchUtils.containsNormalized(cb, root.get("trackingNo"), trackingNo);
    }

    /**
     * Filtra pedidos criados dentro de um intervalo de datas.
     *
     * @param startDate data/hora inicial
     * @param endDate   data/hora final
     * @return Specification correspondente
     */
    public static Specification<OrderMaster> byCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}
