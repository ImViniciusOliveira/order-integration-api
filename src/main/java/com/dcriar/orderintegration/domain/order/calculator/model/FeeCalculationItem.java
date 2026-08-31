package com.dcriar.orderintegration.domain.order.calculator.model;

import java.math.BigDecimal;

/**
 * Record imutável que representa o raio-X contábil de um item individual do pedido durante a auditoria financeira.
 *
 * @param itemId           identificador único do item no marketplace
 * @param itemName         título descritivo do produto anunciado
 * @param modelName        nome descritivo da variação selecionada (SKU)
 * @param unitPrice        preço unitário praticado na venda
 * @param quantity         quantidade de unidades compradas
 * @param subtotal         subtotal da linha (preço unitário * quantidade)
 * @param lowValueItem     indica se o preço unitário é estritamente menor que R$ 8,00
 * @param surchargeApplied valor total da sobretaxa aplicada para itens de baixo valor (R$ 5,00 por unidade)
 */
public record FeeCalculationItem(
        Long itemId,
        String itemName,
        String modelName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        boolean lowValueItem,
        BigDecimal surchargeApplied
) {
}
