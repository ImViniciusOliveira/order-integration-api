package com.dcriar.orderintegration.api.controller.channel;

import com.dcriar.orderintegration.api.dto.request.ChannelStatusUpdateRequest;
import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.api.hateoas.MarketplaceChannelModelAssembler;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.service.MarketplaceChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consulta e gerenciamento de status de canais de venda/marketplaces integrados.
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class MarketplaceChannelController {

    private final MarketplaceChannelService channelService;
    private final MarketplaceChannelModelAssembler channelModelAssembler;

    /**
     * Lista todos os canais de marketplace cadastrados no sistema.
     *
     * @return coleção HATEOAS contendo todos os canais de venda
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<MarketplaceChannelResponse>>> listAll() {
        List<MarketplaceChannel> channels = channelService.listAll();
        return ResponseEntity.ok(channelModelAssembler.toCollectionModel(channels));
    }

    /**
     * Consulta os detalhes de um canal de marketplace pelo seu identificador único.
     *
     * @param id identificador único do canal
     * @return representação HATEOAS do canal consultado
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<MarketplaceChannelResponse>> findById(@PathVariable Long id) {
        MarketplaceChannel channel = channelService.findById(id);
        return ResponseEntity.ok(channelModelAssembler.toModel(channel));
    }

    /**
     * Atualiza o status de ativação (ativo/pausado) de um canal de marketplace.
     *
     * @param id      identificador único do canal
     * @param request payload contendo o novo estado desejado (true/false)
     * @return representação HATEOAS do canal atualizado
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<EntityModel<MarketplaceChannelResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid ChannelStatusUpdateRequest request) {
        MarketplaceChannel updated = channelService.updateStatus(id, request.active());
        return ResponseEntity.ok(channelModelAssembler.toModel(updated));
    }
}
