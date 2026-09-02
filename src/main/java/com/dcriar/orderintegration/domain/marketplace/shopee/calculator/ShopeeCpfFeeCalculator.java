package com.dcriar.orderintegration.domain.marketplace.shopee.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.MarketplaceFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationItem;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
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
 * Implementação Strategy das regras matemáticas e tributárias oficiais da Shopee Brasil
 * para contas de vendedores configuradas como Pessoa Física (CPF).
 * <p>
 * Regras aplicadas (Tabela Oficial Brasil):
 * <ul>
 *   <li>Comissão Base: 14% sobre o subtotal dos itens.</li>
 *   <li>Taxa de Transação/Processamento: 6% sobre o subtotal dos itens.</li>
 *   <li>Taxa Fixa por Item Vendido: R$ 4,00 por unidade.</li>
 *   <li>Sobretaxa de Baixo Valor: R$ 5,00 adicionais por unidade em itens cujo preço unitário seja estritamente menor que R$ 8,00.</li>
 *   <li>Dedução de Frete: Desconto do frete suportado pelo vendedor (se cobrado no extrato).</li>
 *   <li>Tolerância de Arredondamento: R$ 0,05 para compensação de microcentavos bancários.</li>
 * </ul>
 */
@Slf4j
@Component
public class ShopeeCpfFeeCalculator implements MarketplaceFeeCalculator {

    public static final String PLATFORM_CODE = "SHOPEE";
    public static final String RULE_VERSION = "SHOPEE_CPF_BR_2026";

    private static final BigDecimal COMMISSION_RATE_14 = new BigDecimal("0.1400");
    private static final BigDecimal TRANSACTION_RATE_6 = new BigDecimal("0.0600");
    private static final BigDecimal FIXED_FEE_PER_ITEM_4 = new BigDecimal("4.0000");
    private static final BigDecimal LOW_VALUE_THRESHOLD_8 = new BigDecimal("8.0000");
    private static final BigDecimal LOW_VALUE_SURCHARGE_5 = new BigDecimal("5.0000");
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
        BigDecimal lowValueSurchargeTotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

        for (FeeCalculationItem item : auditedItems) {
            subtotalItems = subtotalItems.add(item.subtotal());
            totalQuantityItems += item.quantity();
            lowValueSurchargeTotal = lowValueSurchargeTotal.add(item.surchargeApplied());
        }

        BigDecimal baseCommission = subtotalItems.multiply(COMMISSION_RATE_14).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal transactionFee = subtotalItems.multiply(TRANSACTION_RATE_6).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal fixedItemFee = FIXED_FEE_PER_ITEM_4.multiply(BigDecimal.valueOf(totalQuantityItems)).setScale(4, RoundingMode.HALF_EVEN);

        BigDecimal totalMarketplaceFees = baseCommission
                .add(transactionFee)
                .add(fixedItemFee)
                .add(lowValueSurchargeTotal)
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
                hasDivergence,
                TOLERANCE_AMOUNT.setScale(2, RoundingMode.HALF_EVEN),
                subtotalItems.setScale(2, RoundingMode.HALF_EVEN),
                totalQuantityItems,
                baseCommission.setScale(2, RoundingMode.HALF_EVEN),
                transactionFee.setScale(2, RoundingMode.HALF_EVEN),
                fixedItemFee.setScale(2, RoundingMode.HALF_EVEN),
                lowValueSurchargeTotal.setScale(2, RoundingMode.HALF_EVEN),
                totalMarketplaceFees.setScale(2, RoundingMode.HALF_EVEN),
                resolvedSellerShipping.setScale(2, RoundingMode.HALF_EVEN),
                theoreticalPayout.setScale(2, RoundingMode.HALF_EVEN),
                resolvedActualPayout.setScale(2, RoundingMode.HALF_EVEN),
                calculatedDifference.setScale(2, RoundingMode.HALF_EVEN),
                auditedItems,
                divergenceReason
        );
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
            boolean isLowValue = unitPrice.compareTo(LOW_VALUE_THRESHOLD_8) < 0;
            BigDecimal surchargeApplied = isLowValue
                    ? LOW_VALUE_SURCHARGE_5.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

            items.add(new FeeCalculationItem(
                    itemId,
                    itemName != null ? itemName : "Item sem descrição",
                    modelName != null ? modelName : "",
                    unitPrice.setScale(2, RoundingMode.HALF_EVEN),
                    quantity,
                    itemSubtotal.setScale(2, RoundingMode.HALF_EVEN),
                    isLowValue,
                    surchargeApplied.setScale(2, RoundingMode.HALF_EVEN)
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
