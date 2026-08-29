package com.dcriar.orderintegration.domain.channel.service.impl;

import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.repository.MarketplaceChannelRepository;
import com.dcriar.orderintegration.domain.channel.service.MarketplaceChannelService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementação da camada de serviço para consulta e controle de canais de marketplace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketplaceChannelServiceImpl implements MarketplaceChannelService {

    private final MarketplaceChannelRepository channelRepository;

    @Override
    public List<MarketplaceChannel> listAll() {
        return channelRepository.findAll();
    }

    @Override
    public MarketplaceChannel findById(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Canal de marketplace não encontrado com o ID: " + id));
    }

    @Override
    public MarketplaceChannel findByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResourceNotFoundException("Código de canal inválido ou vazio.");
        }
        String sanitizedCode = code.trim().toUpperCase();
        return channelRepository.findByCode(sanitizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Canal de marketplace não encontrado com o código: " + sanitizedCode));
    }

    @Override
    @Transactional
    public MarketplaceChannel updateStatus(Long id, boolean active) {
        MarketplaceChannel channel = findById(id);
        channel.setActive(active);
        log.info("Status do canal '{}' (ID: {}) atualizado para: active={}", channel.getCode(), channel.getId(), active);
        return channelRepository.save(channel);
    }
}
