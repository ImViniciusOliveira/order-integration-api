package com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.MarketplaceFeeCalculator;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationItem;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.model.MercadoLivreFeeCalculationDetails;
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
 * Implementação Strategy das regras matemáticas e de conciliação financeira do Mercado Livre Brasil.
 * <p>
 * Regras aplicadas (Tabela Oficial Mercado Livre Brasil):
 * <ul>
 *   <li>Comissão Direta: Extração prioritária da taxa oficial calculada por item ({@code sale_fee}),
 *       ou aplicação da taxa padrão de categoria (11,5% clássico / 16% premium).</li>
 *   <li>Custo Fixo por Unidade: Itens com valor estritamente abaixo de R$ 79,00 possuem taxa fixa unitária
 *       caso não tenham sido embutidos no {@code sale_fee} pela API oficial.</li>
 *   <li>Dedução de Frete: Desconto do frete suportado pelo vendedor (se cobrado no extrato).</li>
 *   <li>Tolerância de Arredondamento: R$ 0,05 para compensação de microcentavos bancários.</li>
 * </ul>
 */
@Slf4j
@Component
public class MercadoLivreFeeCalculator implements MarketplaceFeeCalculator {

    public static final String PLATFORM_CODE = "MERCADOLIVRE";
    public static final String RULE_VERSION = "MERCADOLIVRE_BR_2026";

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.1150");
    private static final BigDecimal LOW_VALUE_THRESHOLD_79 = new BigDecimal("79.0000");
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
        BigDecimal extractedSaleFeeTotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

        for (FeeCalculationItem item : auditedItems) {
            subtotalItems = subtotalItems.add(item.subtotal());
            totalQuantityItems += item.quantity();
        }

        // Tentar extrair a comissão exata informada pela API do Mercado Livre (sale_fee)
        BigDecimal saleFeeFromPayload = extractSaleFeeFromMetadata(order.getMetadata());
        BigDecimal totalMarketplaceFees;
        BigDecimal baseCommission;
        BigDecimal fixedItemFee = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

        if (saleFeeFromPayload != null && saleFeeFromPayload.compareTo(BigDecimal.ZERO) > 0) {
            totalMarketplaceFees = saleFeeFromPayload.setScale(4, RoundingMode.HALF_EVEN);
            baseCommission = totalMarketplaceFees;
        } else {
            baseCommission = subtotalItems.multiply(DEFAULT_COMMISSION_RATE).setScale(4, RoundingMode.HALF_EVEN);
            totalMarketplaceFees = baseCommission;
        }

        BigDecimal transactionFee = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal lowValueSurchargeTotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

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
                    "Divergência de R$ %s detectada. Repasse Mercado Livre (%s) difere do cálculo contábil teórico (%s).",
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
                totalMarketplaceFees.setScale(2, RoundingMode.HALF_EVEN),
                resolvedSellerShipping.setScale(2, RoundingMode.HALF_EVEN),
                theoreticalPayout.setScale(2, RoundingMode.HALF_EVEN),
                resolvedActualPayout.setScale(2, RoundingMode.HALF_EVEN),
                calculatedDifference.setScale(2, RoundingMode.HALF_EVEN),
                auditedItems,
                new MercadoLivreFeeCalculationDetails(
                        saleFeeFromPayload,
                        baseCommission.setScale(2, RoundingMode.HALF_EVEN),
                        fixedItemFee.setScale(2, RoundingMode.HALF_EVEN)
                ),
                divergenceReason
        );
    }

    private BigDecimal extractSaleFeeFromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        BigDecimal directFee = extractBigDecimal(metadata, "sale_fee", "saleFee", "marketplace_fee", "marketplaceFee");
        if (directFee != null) {
            return directFee;
        }

        List<Map<String, Object>> rawItemList = resolveRawItemList(metadata);
        if (rawItemList != null && !rawItemList.isEmpty()) {
            BigDecimal totalFee = BigDecimal.ZERO;
            boolean foundAny = false;
            for (Map<String, Object> item : rawItemList) {
                BigDecimal fee = extractBigDecimal(item, "sale_fee", "saleFee");
                if (fee != null) {
                    totalFee = totalFee.add(fee);
                    foundAny = true;
                }
            }
            if (foundAny) {
                return totalFee;
            }
        }
        return null;
    }

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
            Long itemId = extractLong(rawItem, "item_id", "itemId", "id");
            String itemName = extractString(rawItem, "item_name", "itemName", "title", "name");
            String modelName = extractString(rawItem, "model_name", "modelName", "seller_sku", "variation");

            BigDecimal unitPrice = extractBigDecimal(rawItem,
                    "model_discounted_price", "modelDiscountedPrice",
                    "unit_price", "unitPrice", "price", "gross_price");

            if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }
            unitPrice = unitPrice.setScale(4, RoundingMode.HALF_EVEN);

            int quantity = extractInt(rawItem, "model_quantity_purchased", "modelQuantityPurchased", "quantity", "qty");
            if (quantity <= 0) {
                quantity = 1;
            }

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_EVEN);
            boolean isLowValue = unitPrice.compareTo(LOW_VALUE_THRESHOLD_79) < 0;

            items.add(new FeeCalculationItem(
                    itemId,
                    itemName != null ? itemName : "Item Mercado Livre",
                    modelName != null ? modelName : "",
                    unitPrice.setScale(2, RoundingMode.HALF_EVEN),
                    quantity,
                    itemSubtotal.setScale(2, RoundingMode.HALF_EVEN),
                    isLowValue,
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

        Object itemsList = metadata.get("items");
        if (itemsList instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        Object orderItemsList = metadata.get("order_items");
        if (orderItemsList instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        return Collections.emptyList();
    }

    private Long extractLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return Long.valueOf(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
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

    private int extractInt(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return new BigDecimal(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
