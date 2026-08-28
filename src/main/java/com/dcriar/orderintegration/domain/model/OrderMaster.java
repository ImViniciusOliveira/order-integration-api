package com.dcriar.orderintegration.domain.model;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(
    name = "orders_master",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_master_platform_order_sn", columnNames = {"platform", "order_sn"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "shop_id", nullable = false, length = 100)
    private String shopId;

    @Column(name = "order_sn", nullable = false, length = 100)
    private String orderSn;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "tracking_no", length = 100)
    private String trackingNo;

    @Column(name = "estimated_shipping_fee", precision = 15, scale = 4)
    private BigDecimal estimatedShippingFee;

    @Column(name = "escrow_amount", precision = 15, scale = 4)
    private BigDecimal escrowAmount;

    @Column(name = "shipping_fee_borne_by_seller", precision = 15, scale = 4)
    private BigDecimal shippingFeeBorneBySeller;

    @Builder.Default
    @Column(name = "reconciled", nullable = false)
    private boolean reconciled = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public void provisionarEstimativa(String status, BigDecimal estimatedShippingFee, Map<String, Object> metadata) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("O status do pedido não pode ser nulo ou vazio.");
        }
        this.status = status;
        if (estimatedShippingFee != null) {
            this.estimatedShippingFee = estimatedShippingFee;
        }
        if (metadata != null && !metadata.isEmpty()) {
            this.metadata = metadata;
        }
    }

    public void atualizarRastreio(String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new IllegalArgumentException("O código de rastreio não pode ser nulo ou vazio.");
        }
        this.trackingNo = trackingNo;
    }

    public void conciliarEscrow(BigDecimal escrowAmount, BigDecimal shippingFeeBorneBySeller) {
        if (escrowAmount == null || escrowAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor de escrow conciliado não pode ser nulo ou negativo.");
        }
        this.escrowAmount = escrowAmount;
        this.shippingFeeBorneBySeller = shippingFeeBorneBySeller;
        this.reconciled = true;
    }

    public void atualizarStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("O status do pedido não pode ser nulo ou vazio.");
        }
        this.status = status;
    }
}
