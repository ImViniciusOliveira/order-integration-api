package com.dcriar.orderintegration.domain.marketplace.shopee.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.MarketplaceFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationItem;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeAuditStatus;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model.ShopeeFeeCalculationDetails;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.exception.custom.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Executa a auditoria da Shopee usando exclusivamente os componentes financeiros
 * retornados no bloco oficial {@code order_income}.
 */
@Slf4j
@Component
public class ShopeeCpfFeeCalculator implements MarketplaceFeeCalculator {

    public static final String PLATFORM_CODE = "SHOPEE";
    public static final String RULE_VERSION = "SHOPEE_OFFICIAL_ORDER_INCOME";

    private static final BigDecimal TOLERANCE_AMOUNT = new BigDecimal("0.05");

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public FeeCalculationResult calculate(OrderMaster order, BigDecimal actualEscrowAmount, BigDecimal actualSellerShippingFee) {
        if (order == null) {
            throw new BusinessException("O pedido para auditoria financeira não pode ser nulo.");
        }

        List<FeeCalculationItem> auditedItems = extractAndAuditItems(order.getMetadata());

        BigDecimal subtotalItems = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        int totalQuantityItems = 0;

        for (FeeCalculationItem item : auditedItems) {
            subtotalItems = subtotalItems.add(item.subtotal());
            totalQuantityItems += item.quantity();
        }

        Map<String, Object> income = resolveOfficialIncome(order.getMetadata());
        List<String> missingFields = missingOfficialFields(income);
        if (!missingFields.isEmpty()) {
            return incompleteResult(
                    subtotalItems,
                    totalQuantityItems,
                    auditedItems,
                    missingFields
            );
        }

        BigDecimal commissionFee = decimalValue(income, "commission_fee");
        BigDecimal serviceFee = decimalValue(income, "service_fee");
        BigDecimal sellerTransactionFee = decimalValue(income, "seller_transaction_fee");
        BigDecimal otherFees = valueOrZero(decimalValue(income, "escrow_tax"));

        BigDecimal totalMarketplaceFees = commissionFee
                .add(serviceFee)
                .add(sellerTransactionFee)
                .add(otherFees)
                .setScale(4, RoundingMode.HALF_EVEN);

        BigDecimal resolvedSellerShipping = (actualSellerShippingFee != null)
                ? actualSellerShippingFee.setScale(4, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

        BigDecimal theoreticalPayout = subtotalItems
                .subtract(totalMarketplaceFees)
                .subtract(resolvedSellerShipping)
                .setScale(4, RoundingMode.HALF_EVEN);

        BigDecimal resolvedActualPayout = (actualEscrowAmount != null)
                ? actualEscrowAmount.setScale(4, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

        BigDecimal calculatedDifference = resolvedActualPayout.subtract(theoreticalPayout).setScale(4, RoundingMode.HALF_EVEN);

        boolean hasDivergence = calculatedDifference.abs().compareTo(TOLERANCE_AMOUNT) > 0;
        String divergenceReason = null;

        if (hasDivergence) {
            BigDecimal diff2 = calculatedDifference.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal actual2 = resolvedActualPayout.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal theoretical2 = theoreticalPayout.setScale(2, RoundingMode.HALF_EVEN);

            divergenceReason = String.format(
                    "Divergência de R$ %s detectada. Repasse Shopee (%s) difere do cálculo teórico CPF (%s).",
                    diff2.abs().toPlainString(),
                    actual2.toPlainString(),
                    theoretical2.toPlainString()
            );
            log.warn("Auditoria detectou divergência financeira no pedido '{}:{}': {}",
                    order.getPlatform(), order.getOrderSn(), divergenceReason);
        } else {
            log.info("Auditoria financeira do pedido '{}:{}' concluída com sucesso (sem divergências).",
                    order.getPlatform(), order.getOrderSn());
        }

        return new FeeCalculationResult(
                RULE_VERSION,
                OffsetDateTime.now(),
                FeeAuditStatus.COMPLETE,
                hasDivergence,
                TOLERANCE_AMOUNT.setScale(2, RoundingMode.HALF_EVEN),
                subtotalItems.setScale(2, RoundingMode.HALF_EVEN),
                totalQuantityItems,
                totalMarketplaceFees.setScale(2, RoundingMode.HALF_EVEN),
                resolvedSellerShipping.setScale(2, RoundingMode.HALF_EVEN),
                theoreticalPayout.setScale(2, RoundingMode.HALF_EVEN),
                resolvedActualPayout.setScale(2, RoundingMode.HALF_EVEN),
                calculatedDifference.setScale(2, RoundingMode.HALF_EVEN),
                auditedItems,
                new ShopeeFeeCalculationDetails(
                        commissionFee.setScale(2, RoundingMode.HALF_EVEN),
                        serviceFee.setScale(2, RoundingMode.HALF_EVEN),
                        sellerTransactionFee.setScale(2, RoundingMode.HALF_EVEN),
                        otherFees.setScale(2, RoundingMode.HALF_EVEN)
                ),
                divergenceReason
        );
    }

    private FeeCalculationResult incompleteResult(
            BigDecimal subtotalItems,
            int totalQuantityItems,
            List<FeeCalculationItem> auditedItems,
            List<String> missingFields
    ) {
        String reason = "Dados oficiais Shopee ausentes: " + String.join(", ", missingFields);
        return new FeeCalculationResult(
                RULE_VERSION,
                OffsetDateTime.now(),
                FeeAuditStatus.INCOMPLETE,
                false,
                TOLERANCE_AMOUNT.setScale(2, RoundingMode.HALF_EVEN),
                subtotalItems.setScale(2, RoundingMode.HALF_EVEN),
                totalQuantityItems,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                auditedItems,
                new ShopeeFeeCalculationDetails(null, null, null, null),
                reason
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveOfficialIncome(Map<String, Object> metadata) {
        if (metadata == null) {
            return Collections.emptyMap();
        }
        Object details = metadata.get("settlement_financial_details");
        if (!(details instanceof Map<?, ?> detailsMap)) {
            return Collections.emptyMap();
        }
        Object income = detailsMap.get("order_income");
        if (!(income instanceof Map<?, ?> incomeMap)) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) incomeMap;
    }

    private List<String> missingOfficialFields(Map<String, Object> income) {
        List<String> missing = new ArrayList<>();
        addMissing(income, missing, "commission_fee");
        addMissing(income, missing, "service_fee");
        addMissing(income, missing, "seller_transaction_fee");

        Object incomeDetails = income.get("income_details");
        boolean hasShippingInDetails = incomeDetails instanceof Map<?, ?> details
                && (details.containsKey("shipping_fee_borne_by_seller")
                || details.containsKey("actual_shipping_fee")
                || details.containsKey("final_shipping_fee"));
        boolean hasShippingAtIncomeLevel = income.containsKey("actual_shipping_fee")
                || income.containsKey("final_shipping_fee")
                || income.containsKey("shipping_fee_borne_by_seller");
        if (!hasShippingInDetails && !hasShippingAtIncomeLevel) {
            missing.add("frete oficial");
        }
        return missing;
    }

    private void addMissing(Map<String, Object> source, List<String> missing, String key) {
        if (!source.containsKey(key) || decimalValue(source, key) == null) {
            missing.add(key);
        }
    }

    private BigDecimal decimalValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Extrai e audita a lista de itens comprados a partir da estrutura JSONB flexível.
     *
     * @param metadata mapa flexível contendo os dados brutos recebidos da Shopee
     * @return lista de itens normalizados e auditados
     */
    @SuppressWarnings("unchecked")
    private List<FeeCalculationItem> extractAndAuditItems(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rawItemList = resolveRawItemList(metadata);
        if (rawItemList == null || rawItemList.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeeCalculationItem> items = new ArrayList<>();
        for (Map<String, Object> rawItem : rawItemList) {
            Long itemId = extractLong(rawItem, "item_id", "itemId");
            String itemName = extractString(rawItem, "item_name", "itemName", "name");
            String modelName = extractString(rawItem, "model_name", "modelName", "variation_name", "variation");

            BigDecimal unitPrice = extractBigDecimal(rawItem,
                    "model_discounted_price", "modelDiscountedPrice",
                    "model_original_price", "modelOriginalPrice",
                    "discounted_price", "original_price", "price", "item_price");

            if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }
            unitPrice = unitPrice.setScale(4, RoundingMode.HALF_EVEN);

            int quantity = extractInt(rawItem, "model_quantity_purchased", "modelQuantityPurchased", "quantity_purchased", "quantity", "qty");
            if (quantity <= 0) {
                quantity = 1;
            }

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_EVEN);
            items.add(new FeeCalculationItem(
                    itemId,
                    itemName != null ? itemName : "Item sem descrição",
                    modelName != null ? modelName : "",
                    unitPrice.setScale(2, RoundingMode.HALF_EVEN),
                    quantity,
                    itemSubtotal.setScale(2, RoundingMode.HALF_EVEN),
                    false,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
            ));
        }

        return Collections.unmodifiableList(items);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveRawItemList(Map<String, Object> metadata) {
        Object directList = metadata.get("item_list");
        if (directList instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        Object responseObj = metadata.get("response");
        if (responseObj instanceof Map<?, ?> responseMap) {
            Object orderListObj = responseMap.get("order_list");
            if (orderListObj instanceof List<?> orderList && !orderList.isEmpty()) {
                Object firstOrder = orderList.getFirst();
                if (firstOrder instanceof Map<?, ?> orderMap) {
                    Object nestedItemList = orderMap.get("item_list");
                    if (nestedItemList instanceof List<?> nestedList) {
                        return (List<Map<String, Object>>) nestedList;
                    }
                }
            }
        }

        Object orderListObj = metadata.get("order_list");
        if (orderListObj instanceof List<?> orderList && !orderList.isEmpty()) {
            Object firstOrder = orderList.getFirst();
            if (firstOrder instanceof Map<?, ?> orderMap) {
                Object nestedItemList = orderMap.get("item_list");
                if (nestedItemList instanceof List<?> nestedList) {
                    return (List<Map<String, Object>>) nestedList;
                }
            }
        }

        Object itemsObj = metadata.get("items");
        if (itemsObj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        return Collections.emptyList();
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return null;
    }

    private Long extractLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number num) {
                return num.longValue();
            } else if (val != null) {
                try {
                    return Long.parseLong(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private int extractInt(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number num) {
                return num.intValue();
            } else if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 1;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof BigDecimal bd) {
                return bd;
            } else if (val instanceof Number num) {
                return BigDecimal.valueOf(num.doubleValue());
            } else if (val != null) {
                try {
                    return new BigDecimal(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
