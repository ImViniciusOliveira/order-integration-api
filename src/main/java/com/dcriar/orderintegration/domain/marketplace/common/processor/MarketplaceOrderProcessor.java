package com.dcriar.orderintegration.domain.marketplace.common.processor;

import com.dcriar.orderintegration.domain.marketplace.common.model.OrderProcessingResult;

import java.util.Map;

/**
 * Contrato Strategy para extração e normalização de eventos brutos de pedidos provenientes de diferentes marketplaces.
 */
public interface MarketplaceOrderProcessor {

    /**
     * Indica se a implementação suporta a plataforma informada.
     *
     * @param platform código da plataforma (ex: "SHOPEE")
     * @return {@code true} se suportar, {@code false} caso contrário
     */
    boolean supports(String platform);

    /**
     * Processa o payload bruto recebido da plataforma e o normaliza em um {@link OrderProcessingResult}.
     *
     * @param shopId  identificador opcional da loja recebido nos headers ou na URL
     * @param payload estrutura JSON completa enviada pelo marketplace
     * @return o resultado processado e normalizado
     */
    OrderProcessingResult process(String shopId, Map<String, Object> payload);
}
