package com.dcriar.orderintegration.domain.order.entity;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Entidade que representa o Event Store imutável de eventos e webhooks brutos
 * recebidos de plataformas e marketplaces.
 * <p>
 * Garante rastreabilidade, auditoria e reprocessamento contábil.
 */
@Entity
@Table(name = "marketplace_raw_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceRawEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "shop_id", length = 100)
    private String shopId;

    @Column(name = "order_sn", length = 100)
    private String orderSn;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;

    /**
     * Cria e instancia um novo evento bruto de marketplace pronto para persistência imutável.
     *
     * @param platform    plataforma de origem (ex: "SHOPEE")
     * @param shopId      identificador da loja
     * @param orderSn     número do pedido
     * @param eventType   tipo do evento recebido (ex: status)
     * @param payloadJson payload JSON bruto
     * @return a entidade instanciada
     */
    public static MarketplaceRawEvent criarEvento(
            String platform,
            String shopId,
            String orderSn,
            String eventType,
            Map<String, Object> payloadJson
    ) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("A plataforma de origem é obrigatória.");
        }
        if (payloadJson == null || payloadJson.isEmpty()) {
            throw new IllegalArgumentException("O payload JSON do evento não pode ser nulo ou vazio.");
        }

        return MarketplaceRawEvent.builder()
                .platform(platform.toUpperCase())
                .shopId(shopId)
                .orderSn(orderSn)
                .eventType(eventType)
                .payloadJson(payloadJson)
                .build();
    }
}
