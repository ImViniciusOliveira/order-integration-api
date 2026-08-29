package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para validar os links gerados pelo MarketplaceChannelModelAssembler.
 */
class MarketplaceChannelModelAssemblerTest {

    private final MarketplaceChannelModelAssembler assembler = new MarketplaceChannelModelAssembler();

    @Test
    @DisplayName("Deve gerar modelo HATEOAS com links self, toggle-status e collection")
    void deveGerarModeloComLinksHateoas() {
        // Arrange
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(
                1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now()
        );

        // Act
        EntityModel<MarketplaceChannelResponse> model = assembler.toModel(response);

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
    @DisplayName("Deve gerar CollectionModel com links HATEOAS para lista de canais")
    void deveGerarCollectionModelParaListaDeCanais() {
        // Arrange
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(
                1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now()
        );

        // Act
        CollectionModel<EntityModel<MarketplaceChannelResponse>> collectionModel = assembler.toCollectionModel(List.of(response));

        // Assert
        assertThat(collectionModel).isNotNull();
        assertThat(collectionModel.getContent()).hasSize(1);
        assertThat(collectionModel.hasLink("self")).isTrue();
        assertThat(collectionModel.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/channels");
    }
}
