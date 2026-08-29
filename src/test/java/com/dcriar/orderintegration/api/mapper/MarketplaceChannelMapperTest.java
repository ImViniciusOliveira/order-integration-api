package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.request.MarketplaceChannelRequest;
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
    @DisplayName("Deve converter MarketplaceChannelRequest para entidade MarketplaceChannel")
    void deveConverterRequestParaEntidade() {
        // Arrange
        MarketplaceChannelRequest request = new MarketplaceChannelRequest("SHOPEE_OFICIAL", "Shopee Oficial", true);

        // Act
        MarketplaceChannel entity = mapper.toEntity(request);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getCode()).isEqualTo("SHOPEE_OFICIAL");
        assertThat(entity.getName()).isEqualTo("Shopee Oficial");
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve converter entidade MarketplaceChannel para MarketplaceChannelResponse")
    void deveConverterEntidadeParaResponse() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder()
                .id(1L)
                .code("SHOPEE_OFICIAL")
                .name("Shopee Oficial")
                .active(true)
                .build();

        // Act
        MarketplaceChannelResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("SHOPEE_OFICIAL");
        assertThat(response.name()).isEqualTo("Shopee Oficial");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Deve converter lista de entidades para lista de responses")
    void deveConverterListaDeEntidadesParaResponses() {
        // Arrange
        MarketplaceChannel channel1 = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        MarketplaceChannel channel2 = MarketplaceChannel.builder().id(2L).code("MERCADO_LIVRE").name("Mercado Livre").active(true).build();

        // Act
        List<MarketplaceChannelResponse> responses = mapper.toResponseList(List.of(channel1, channel2));

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).code()).isEqualTo("SHOPEE");
        assertThat(responses.get(1).code()).isEqualTo("MERCADO_LIVRE");
    }
}
