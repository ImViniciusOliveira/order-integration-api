package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.api.mapper.MarketplaceChannelMapper;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Assembler HATEOAS responsável por converter a entidade JPA {@link MarketplaceChannel}
 * no DTO {@link MarketplaceChannelResponse} enriquecido com links de hipermídia e auto-navegação.
 */
@Component
@RequiredArgsConstructor
public class MarketplaceChannelModelAssembler implements RepresentationModelAssembler<MarketplaceChannel, EntityModel<MarketplaceChannelResponse>> {

    private static final String BASE_URI = "/api/v1/channels";

    private final MarketplaceChannelMapper mapper;

    @Override
    public EntityModel<MarketplaceChannelResponse> toModel(MarketplaceChannel entity) {
        if (entity == null) {
            return null;
        }

        MarketplaceChannelResponse response = mapper.toResponse(entity);
        EntityModel<MarketplaceChannelResponse> model = EntityModel.of(response);

        if (entity.getId() != null) {
            model.add(Link.of(BASE_URI + "/" + entity.getId()).withSelfRel());
            model.add(Link.of(BASE_URI + "/" + entity.getId() + "/status").withRel("toggle-status"));
        }

        model.add(Link.of(BASE_URI).withRel("collection"));

        return model;
    }

    @Override
    public CollectionModel<EntityModel<MarketplaceChannelResponse>> toCollectionModel(Iterable<? extends MarketplaceChannel> entities) {
        List<EntityModel<MarketplaceChannelResponse>> models = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .toList();

        return CollectionModel.of(models, Link.of(BASE_URI).withSelfRel());
    }
}
