package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.mapper;

import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte as respostas financeiras encadeadas do Mercado Livre para o settlement interno.
 */
@Component
public class MercadoLivreSettlementResponseMapper {

    private static final String PLATFORM_CODE = "MERCADOLIVRE";

    /**
     * Mapeia pedido, envio e pagamento do Mercado Livre.
     *
     * @param accountId identificador da conta consultada
     * @param orderId identificador do pedido
     * @param order    resposta do recurso de pedidos
     * @param shipment resposta do recurso de envios, quando disponível
     * @param shipmentCosts custos oficiais do envio, quando disponíveis
     * @param payment  resposta detalhada do pagamento, quando disponível
     * @return settlement normalizado
     */
    public MarketplaceSettlement map(
            String accountId,
            String orderId,
            Map<String, Object> order,
            Map<String, Object> shipment,
            Map<String, Object> shipmentCosts,
            Map<String, Object> payment
    ) {
        Map<String, Object> financialDetails = new LinkedHashMap<>();
        financialDetails.put("order", safeMap(order));
        financialDetails.put("shipment", safeMap(shipment));
        financialDetails.put("shipment_costs", safeMap(shipmentCosts));
        financialDetails.put("payment", safeMap(payment));

        BigDecimal netAmount = decimalValue(payment, "transaction_details", "net_received_amount");
        BigDecimal grossAmount = decimalValue(order, "total_amount");
        BigDecimal commissionAmount = sumSaleFees(order);
        BigDecimal shippingFee = sumSenderCosts(shipmentCosts);
        BigDecimal transactionFee = decimalValue(payment, "fee_details", "total");

        String pendingReason = pendingReason(payment, netAmount);
        if (pendingReason != null) {
            return pending(accountId, orderId, financialDetails, pendingReason);
        }

        return new MarketplaceSettlement(
                SettlementStatus.AVAILABLE,
                PLATFORM_CODE,
                orderId,
                accountId,
                grossAmount,
                netAmount,
                commissionAmount,
                valueOrZero(transactionFee),
                valueOrZero(shippingFee),
                BigDecimal.ZERO,
                financialDetails,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * Mantem compatibilidade para consumidores que ainda nao fornecem os custos do shipment.
     *
     * @param accountId identificador da conta consultada
     * @param orderId identificador do pedido
     * @param order resposta do pedido
     * @param shipment resposta do envio
     * @param payment resposta do pagamento
     * @return settlement normalizado sem custo oficial de envio
     */
    public MarketplaceSettlement map(
            String accountId,
            String orderId,
            Map<String, Object> order,
            Map<String, Object> shipment,
            Map<String, Object> payment
    ) {
        return map(accountId, orderId, order, shipment, Map.of(), payment);
    }

    private MarketplaceSettlement pending(
            String accountId,
            String orderId,
            Map<String, Object> financialDetails,
            String reason
    ) {
        return new MarketplaceSettlement(
                SettlementStatus.PENDING,
                PLATFORM_CODE,
                orderId,
                accountId,
                decimalValue(safeMap(financialDetails.get("order")), "total_amount"),
                null,
                null,
                null,
                null,
                null,
                financialDetails,
                OffsetDateTime.now(),
                reason
        );
    }

    private String pendingReason(Map<String, Object> payment, BigDecimal netAmount) {
        if (payment.isEmpty()) {
            return "Detalhes do pagamento ainda não disponíveis";
        }
        if (!"approved".equalsIgnoreCase(stringValue(payment, "status"))) {
            return "Pagamento Mercado Livre ainda não aprovado";
        }
        if (netAmount == null) {
            return "Valor líquido do pagamento ainda não disponível";
        }

        String releaseDate = stringValue(payment, "money_release_date");
        if (releaseDate == null) {
            releaseDate = stringValue(payment, "transaction_details", "money_release_date");
        }
        if (releaseDate != null && parseDate(releaseDate).isAfter(OffsetDateTime.now())) {
            return "Pagamento aprovado, mas o dinheiro ainda não foi liberado";
        }
        return null;
    }

    private BigDecimal sumSaleFees(Map<String, Object> order) {
        Object items = order.get("order_items");
        if (!(items instanceof List<?> list)) {
            return BigDecimal.ZERO;
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> decimalValue(item, "sale_fee"))
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSenderCosts(Map<String, Object> shipmentCosts) {
        Object senders = shipmentCosts.get("senders");
        if (!(senders instanceof List<?> list)) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean found = false;
        for (Object sender : list) {
            if (!(sender instanceof Map<?, ?> senderMap)) {
                continue;
            }
            Object cost = senderMap.get("cost");
            if (cost == null) {
                continue;
            }
            try {
                total = total.add(new BigDecimal(cost.toString()));
                found = true;
            } catch (NumberFormatException ignored) {
                // Valores inválidos não podem ser usados como custo financeiro.
            }
        }
        return found ? total : null;
    }

    private BigDecimal decimalValue(Map<String, Object> source, String... path) {
        Object value = valueAt(source, path);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object valueAt(Map<String, Object> source, String... path) {
        Object current = source;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    private String stringValue(Map<String, Object> source, String... path) {
        Object value = valueAt(source, path);
        return value == null ? null : value.toString();
    }

    private OffsetDateTime parseDate(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return OffsetDateTime.ofInstant(java.time.Instant.parse(value), ZoneOffset.UTC);
        }
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object source) {
        return source instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
