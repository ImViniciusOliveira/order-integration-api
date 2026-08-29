package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para validar as conversões do MarketplaceChannelMapper.
 */
class MarketplaceChannelMapperTest {

    private final MarketplaceChannelMapper mapper = Mappers.getMapper(MarketplaceChannelMapper.class);

    @Test
    @DisplayName("Deve converter entidade MarketplaceChannel para MarketplaceChannelResponse")
    void deveConverterEntidadeParaResponse() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder()
                .id(1L)
                .code("SHOPEE")
                .name("Shopee")
                .active(true)
                .build();

        // Act
        MarketplaceChannelResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("SHOPEE");
        assertThat(response.name()).isEqualTo("Shopee");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Deve converter lista de entidades para lista de responses")
    void deveConverterListaDeEntidadesParaResponses() {
        // Arrange
        MarketplaceChannel channel1 = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();

        // Act
        List<MarketplaceChannelResponse> responses = mapper.toResponseList(List.of(channel1));

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo("SHOPEE");
    }
}
