package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.domain.order.entity.OrderMaster;

import java.util.Map;

/**
 * Contrato de serviço para ingestão e processamento de eventos de marketplaces.
 */
public interface MarketplaceIngestionService {

    /**
     * Processa a chegada de um evento bruto de marketplace recebido via webhook ou n8n.
     *
     * @param platform código da plataforma de origem (ex: "SHOPEE")
     * @param shopId   identificador opcional da loja
     * @param payload  estrutura JSON completa do evento recebido
     * @return a entidade {@link OrderMaster} persistida e atualizada
     */
    OrderMaster ingestEvent(String platform, String shopId, Map<String, Object> payload);
}
