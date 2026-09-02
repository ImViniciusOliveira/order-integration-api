package com.dcriar.orderintegration.domain.marketplace.common.calculator.mapper;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationItem;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationDetails;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.model.MercadoLivreFeeCalculationDetails;
import com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model.ShopeeFeeCalculationDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper de domínio responsável pela conversão e estruturação do resultado contábil
 * da auditoria financeira ({@link FeeCalculationResult}) em formato de mapa serializável para JSONB.
 */
@Component
public class FeeCalculationMapper {

    /**
     * Converte o resultado consolidado da auditoria contábil em um mapa estruturado
     * para persistência direta na coluna {@code metadata} (JSONB) do pedido mestre.
     *
     * @param result resultado da prova real contábil
     * @return mapa estruturado contendo resumo financeiro e itens auditados, ou mapa vazio se nulo
     */
    public Map<String, Object> toMap(FeeCalculationResult result) {
        if (result == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("versao_regra", result.ruleVersion());
        root.put("data_auditoria", result.auditDate() != null ? result.auditDate().toString() : null);
        root.put("has_divergence", result.hasDivergence());
        root.put("tolerancia_centavos", result.tolerance());

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("subtotal_itens", result.subtotalItems());
        resumo.put("quantidade_total_itens", result.totalQuantityItems());
        resumo.put("total_taxas_marketplace", result.totalMarketplaceFees());
        resumo.put("frete_vendedor", result.sellerShippingFee());
        resumo.put("repasse_liquido_teorico", result.theoreticalPayout());
        resumo.put("repasse_liquido_real", result.actualPayout());
        resumo.put("diferenca_apurada", result.calculatedDifference());
        root.put("resumo_financeiro", resumo);
        root.put("detalhes_plataforma", mapPlatformDetails(result.platformDetails()));

        if (result.auditedItems() != null) {
            List<Map<String, Object>> itemsList = result.auditedItems().stream()
                    .map(this::mapAuditedItem)
                    .toList();
            root.put("itens_auditados", itemsList);
        }

        root.put("motivo_divergencia", result.divergenceReason());
        return root;
    }

    private Map<String, Object> mapPlatformDetails(FeeCalculationDetails details) {
        if (details instanceof ShopeeFeeCalculationDetails shopee) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("comissao_base_14", shopee.baseCommission14());
            result.put("taxa_transacao_6", shopee.transactionFee6());
            result.put("taxa_fixa_item_4", shopee.fixedItemFee4());
            result.put("sobretaxa_baixo_valor_5", shopee.lowValueSurcharge5());
            return result;
        }
        if (details instanceof MercadoLivreFeeCalculationDetails mercadoLivre) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sale_fee", mercadoLivre.saleFee());
            result.put("comissao_contingencial", mercadoLivre.defaultCommission());
            result.put("tarifa_fixa_unitaria", mercadoLivre.fixedUnitFee());
            return result;
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> mapAuditedItem(FeeCalculationItem item) {
        Map<String, Object> itemMap = new LinkedHashMap<>();
        itemMap.put("item_id", item.itemId());
        itemMap.put("item_name", item.itemName());
        itemMap.put("model_name", item.modelName());
        itemMap.put("preco_unitario", item.unitPrice());
        itemMap.put("quantidade", item.quantity());
        itemMap.put("subtotal_item", item.subtotal());
        itemMap.put("item_baixo_valor", item.lowValueItem());
        itemMap.put("sobretaxa_aplicada", item.surchargeApplied());
        return itemMap;
    }
}
