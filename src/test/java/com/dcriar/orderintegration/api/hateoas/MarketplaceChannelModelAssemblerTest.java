package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.api.mapper.MarketplaceChannelMapper;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar a conversão de Entidade para Model HATEOAS no MarketplaceChannelModelAssembler.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceChannelModelAssemblerTest {

    @Mock
    private MarketplaceChannelMapper mapper;

    private MarketplaceChannelModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new MarketplaceChannelModelAssembler(mapper);
    }

    @Test
    @DisplayName("Deve converter Entidade JPA para EntityModel com links self, toggle-status e collection")
    void deveConverterEntidadeParaModeloHateoas() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder()
                .id(1L)
                .code("SHOPEE")
                .name("Shopee")
                .active(true)
                .build();

        MarketplaceChannelResponse response = new MarketplaceChannelResponse(
                1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(mapper.toResponse(entity)).thenReturn(response);

        // Act
        EntityModel<MarketplaceChannelResponse> model = assembler.toModel(entity);

        // Assert
        assertThat(model).isNotNull();
        assertThat(model.getContent()).isEqualTo(response);
        assertThat(model.hasLink("self")).isTrue();
        assertThat(model.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/channels/1");
        assertThat(model.hasLink("toggle-status")).isTrue();
        assertThat(model.getLink("toggle-status").orElseThrow().getHref()).isEqualTo("/api/v1/channels/1/status");
        assertThat(model.hasLink("collection")).isTrue();
        assertThat(model.getLink("collection").orElseThrow().getHref()).isEqualTo("/api/v1/channels");
    }

    @Test
    @DisplayName("Deve converter lista de entidades para CollectionModel com links HATEOAS")
    void deveConverterListaDeEntidadesParaCollectionModel() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now());

        when(mapper.toResponse(any(MarketplaceChannel.class))).thenReturn(response);

        // Act
        CollectionModel<EntityModel<MarketplaceChannelResponse>> collectionModel = assembler.toCollectionModel(List.of(entity));

        // Assert
        assertThat(collectionModel).isNotNull();
        assertThat(collectionModel.getContent()).hasSize(1);
        assertThat(collectionModel.hasLink("self")).isTrue();
        assertThat(collectionModel.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/channels");
    }
}
