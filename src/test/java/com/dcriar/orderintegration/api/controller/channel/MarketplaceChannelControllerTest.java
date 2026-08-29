package com.dcriar.orderintegration.api.controller.channel;

import com.dcriar.orderintegration.api.dto.request.ChannelStatusUpdateRequest;
import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.api.hateoas.MarketplaceChannelModelAssembler;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.service.MarketplaceChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validação dos métodos do MarketplaceChannelController.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceChannelControllerTest {

    @Mock
    private MarketplaceChannelService channelService;

    @Mock
    private MarketplaceChannelModelAssembler channelModelAssembler;

    private MarketplaceChannelController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketplaceChannelController(channelService, channelModelAssembler);
    }

    @Test
    @DisplayName("GET /api/v1/channels - Deve listar todos os canais com status 200 OK")
    void deveListarTodosOsCanais() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now());
        EntityModel<MarketplaceChannelResponse> model = EntityModel.of(response, Link.of("/api/v1/channels/1").withSelfRel());
        CollectionModel<EntityModel<MarketplaceChannelResponse>> collectionModel = CollectionModel.of(List.of(model));

        when(channelService.listAll()).thenReturn(List.of(entity));
        when(channelModelAssembler.toCollectionModel(any())).thenReturn(collectionModel);

        // Act
        ResponseEntity<CollectionModel<EntityModel<MarketplaceChannelResponse>>> result = controller.listAll();

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id} - Deve retornar detalhes do canal por ID com status 200 OK")
    void deveBuscarCanalPorId() {
        // Arrange
        MarketplaceChannel entity = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(true).build();
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(1L, "SHOPEE", "Shopee", true, OffsetDateTime.now(), OffsetDateTime.now());
        EntityModel<MarketplaceChannelResponse> model = EntityModel.of(response, Link.of("/api/v1/channels/1").withSelfRel());

        when(channelService.findById(1L)).thenReturn(entity);
        when(channelModelAssembler.toModel(entity)).thenReturn(model);

        // Act
        ResponseEntity<EntityModel<MarketplaceChannelResponse>> result = controller.findById(1L);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEqualTo(response);
    }

    @Test
    @DisplayName("PATCH /api/v1/channels/{id}/status - Deve atualizar o status do canal com sucesso")
    void deveAtualizarStatusDoCanal() {
        // Arrange
        ChannelStatusUpdateRequest request = new ChannelStatusUpdateRequest(false);
        MarketplaceChannel updatedEntity = MarketplaceChannel.builder().id(1L).code("SHOPEE").name("Shopee").active(false).build();
        MarketplaceChannelResponse response = new MarketplaceChannelResponse(1L, "SHOPEE", "Shopee", false, OffsetDateTime.now(), OffsetDateTime.now());
        EntityModel<MarketplaceChannelResponse> model = EntityModel.of(response, Link.of("/api/v1/channels/1").withSelfRel());

        when(channelService.updateStatus(1L, false)).thenReturn(updatedEntity);
        when(channelModelAssembler.toModel(updatedEntity)).thenReturn(model);

        // Act
        ResponseEntity<EntityModel<MarketplaceChannelResponse>> result = controller.updateStatus(1L, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEqualTo(response);
    }
}
