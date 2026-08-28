package com.dcriar.orderintegration.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "marketplace_raw_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceRawEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "shop_id", nullable = false, length = 100)
    private String shopId;

    @Column(name = "order_sn", length = 100)
    private String orderSn;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static MarketplaceRawEvent criarEvento(String platform, String shopId, String orderSn, String eventType, Map<String, Object> payloadJson) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("A plataforma do evento não pode ser nula ou vazia.");
        }
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalArgumentException("O ID da loja do evento não pode ser nulo ou vazio.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("O tipo de evento não pode ser nulo ou vazio.");
        }
        if (payloadJson == null) {
            throw new IllegalArgumentException("O payload JSON do evento não pode ser nulo.");
        }
        return MarketplaceRawEvent.builder()
                .platform(platform.trim().toUpperCase())
                .shopId(shopId.trim())
                .orderSn(orderSn != null ? orderSn.trim() : null)
                .eventType(eventType.trim())
                .payloadJson(payloadJson)
                .build();
    }
}
