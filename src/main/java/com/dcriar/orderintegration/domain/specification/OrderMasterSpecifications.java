package com.dcriar.orderintegration.domain.specification;

import com.dcriar.orderintegration.domain.common.PostgresSearchUtils;
import com.dcriar.orderintegration.domain.model.OrderMaster;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Construtor de especificações dinâmicas (Spring Data JPA Specifications) para consultas
 * e filtros avançados na entidade mestre de pedidos {@link OrderMaster}.
 */
public final class OrderMasterSpecifications {

    private OrderMasterSpecifications() {
    }

    /**
     * Filtra pedidos pelo código da plataforma de origem.
     *
     * @param platform o código do marketplace (ex: "SHOPEE", "TIKTOK")
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byPlatform(String platform) {
        return (root, query, cb) -> {
            if (platform == null || platform.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("platform")), platform.trim().toUpperCase());
        };
    }

    /**
     * Filtra pedidos pelo identificador da loja no marketplace.
     *
     * @param shopId o identificador da loja
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byShopId(String shopId) {
        return (root, query, cb) -> {
            if (shopId == null || shopId.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("shopId"), shopId.trim());
        };
    }

    /**
     * Filtra pedidos pelo status atual do ciclo de vida.
     *
     * @param status o status do pedido (ex: "READY_TO_SHIP", "COMPLETED")
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
        };
    }

    /**
     * Filtra pedidos pelo estado de conciliação financeira de Escrow.
     *
     * @param reconciled {@code true} para pedidos já conciliados, {@code false} para pendentes
     * @return a especificação JPA correspondente
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
     * Realiza busca textual normalizada (case-insensitive e sem acentos) no número do pedido.
     *
     * @param orderSn o termo ou número do pedido para busca
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byOrderSn(String orderSn) {
        return (root, query, cb) -> PostgresSearchUtils.containsNormalized(cb, root.get("orderSn"), orderSn);
    }

    /**
     * Realiza busca textual normalizada (case-insensitive e sem acentos) no código de rastreio.
     *
     * @param trackingNo o código de rastreamento do pacote
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byTrackingNo(String trackingNo) {
        return (root, query, cb) -> PostgresSearchUtils.containsNormalized(cb, root.get("trackingNo"), trackingNo);
    }

    /**
     * Filtra pedidos criados dentro de um intervalo temporal de datas/horas.
     *
     * @param start data e hora de início do intervalo (inclusive)
     * @param end   data e hora de término do intervalo (inclusive)
     * @return a especificação JPA correspondente
     */
    public static Specification<OrderMaster> byCreatedAtBetween(OffsetDateTime start, OffsetDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("createdAt"), start, end);
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            }
            if (end != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), end);
            }
            return cb.conjunction();
        };
    }
}
