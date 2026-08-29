package com.dcriar.orderintegration.domain.channel.service;

import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;

import java.util.List;

/**
 * Contrato de serviço para consulta e controle de status operacional dos canais de marketplace.
 */
public interface MarketplaceChannelService {

    /**
     * Retorna a lista de todos os canais de marketplace configurados no sistema.
     *
     * @return lista completa de canais
     */
    List<MarketplaceChannel> listAll();

    /**
     * Busca um canal de marketplace pelo seu identificador único.
     *
     * @param id identificador do canal
     * @return entidade do canal encontrada
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException caso o canal não exista
     */
    MarketplaceChannel findById(Long id);

    /**
     * Busca um canal de marketplace pelo seu código identificador (ex: "SHOPEE").
     *
     * @param code código mnemônico do canal
     * @return entidade do canal encontrada
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException caso o canal não exista
     */
    MarketplaceChannel findByCode(String code);

    /**
     * Atualiza o estado operacional do canal de marketplace (ativar ou pausar integração).
     *
     * @param id     identificador do canal
     * @param active novo estado desejado (true para ativo, false para pausado)
     * @return entidade do canal com o estado atualizado
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException caso o canal não exista
     */
    MarketplaceChannel updateStatus(Long id, boolean active);
}
