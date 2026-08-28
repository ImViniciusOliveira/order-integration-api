package com.dcriar.orderintegration.service.processor;

import java.util.Map;

/**
 * Interface que define o contrato do padrão de projeto Strategy para processamento
 * e normalização de eventos e webhooks de diferentes marketplaces.
 */
public interface MarketplaceOrderProcessor {

    /**
     * Verifica se esta implementação atende ao código da plataforma informada.
     *
     * @param platform o código do marketplace (ex: "SHOPEE", "TIKTOK")
     * @return {@code true} se o processador suportar a plataforma, {@code false} caso contrário
     */
    boolean supports(String platform);

    /**
     * Processa o payload bruto recebido e extrai os dados normalizados de negócio.
     *
     * @param shopId  identificador opcional da loja recebido no cabeçalho ou payload
     * @param payload mapa contendo a estrutura JSON do evento do marketplace
     * @return o resultado padronizado com os dados extraídos
     */
    OrderProcessingResult process(String shopId, Map<String, Object> payload);
}
