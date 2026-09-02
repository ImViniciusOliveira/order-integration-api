package com.dcriar.orderintegration.domain.channel.service;

import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.repository.MarketplaceChannelRepository;
import com.dcriar.orderintegration.domain.channel.service.impl.MarketplaceChannelServiceImpl;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para o MarketplaceChannelServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceChannelServiceTest {

    @Mock
    private MarketplaceChannelRepository channelRepository;

    @InjectMocks
    private MarketplaceChannelServiceImpl channelService;

    @Test
    @DisplayName("Deve retornar todos os canais cadastrados com sucesso")
    void deveListarTodosOsCanais() {
        // Arrange
        MarketplaceChannel shopee = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        when(channelRepository.findAll()).thenReturn(List.of(shopee));

        // Act
        List<MarketplaceChannel> canais = channelService.listAll();

        // Assert
        assertThat(canais).hasSize(1);
        assertThat(canais.get(0).getCode()).isEqualTo("SHOPEE");
        verify(channelRepository).findAll();
    }

    @Test
    @DisplayName("Deve buscar canal por ID com sucesso")
    void deveBuscarCanalPorIdComSucesso() {
        // Arrange
        MarketplaceChannel shopee = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(shopee));

        // Act
        MarketplaceChannel canal = channelService.findById(1L);

        // Assert
        assertThat(canal).isNotNull();
        assertThat(canal.getId()).isEqualTo(1L);
        assertThat(canal.getCode()).isEqualTo("SHOPEE");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar canal por ID inexistente")
    void deveLancarExcecaoAoBuscarCanalPorIdInexistente() {
        // Arrange
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> channelService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Canal de marketplace não encontrado com o ID: 99");
    }

    @Test
    @DisplayName("Deve buscar canal por código com sucesso usando o código normalizado")
    void deveBuscarCanalPorCodigoComSucesso() {
        // Arrange
        MarketplaceChannel shopee = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        when(channelRepository.findByCode("SHOPEE")).thenReturn(Optional.of(shopee));

        // Act
        MarketplaceChannel canal = channelService.findByCode("SHOPEE");

        // Assert
        assertThat(canal).isNotNull();
        assertThat(canal.getCode()).isEqualTo("SHOPEE");
        verify(channelRepository).findByCode("SHOPEE");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar código inexistente")
    void deveLancarExcecaoAoBuscarCodigoInexistente() {
        // Arrange
        when(channelRepository.findByCode("TIKTOK")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> channelService.findByCode("TIKTOK"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Canal de marketplace não encontrado com o código: TIKTOK");
    }

    @Test
    @DisplayName("Deve atualizar status do canal (desativar/ativar) com sucesso")
    void deveAtualizarStatusDoCanal() {
        // Arrange
        MarketplaceChannel shopee = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(shopee));
        when(channelRepository.save(any(MarketplaceChannel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MarketplaceChannel atualizado = channelService.updateStatus(1L, false);

        // Assert
        assertThat(atualizado).isNotNull();
        assertThat(atualizado.isActive()).isFalse();
        verify(channelRepository).save(shopee);
    }
}
