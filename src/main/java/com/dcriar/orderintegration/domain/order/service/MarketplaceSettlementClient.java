package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.domain.order.model.MarketplaceSettlement;

/**
 * Contrato para consulta de dados financeiros pós-venda em um marketplace específico.
 * <p>
 * As implementações concretas devem cuidar de autenticação, assinatura, chamadas HTTP,
 * renovação de tokens e mapeamento da resposta externa para {@link MarketplaceSettlement}.
 */
public interface MarketplaceSettlementClient {

    /**
     * Verifica se o client atende à plataforma informada.
     *
     * @param platform código normalizado ou bruto da plataforma
     * @return {@code true} quando o client suporta a plataforma
     */
    boolean supports(String platform);

    /**
     * Consulta o settlement financeiro de um pedido.
     *
     * @param accountId identificador da loja ou vendedor na plataforma
     * @param orderId   identificador do pedido na plataforma
     * @return resultado financeiro padronizado
     */
    MarketplaceSettlement fetchSettlement(String accountId, String orderId);
}
