package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Assembler HATEOAS responsável por enriquecer o DTO {@link MarketplaceChannelResponse}
 * com links de hipermídia e auto-navegação (HATEOAS).
 */
@Component
public class MarketplaceChannelModelAssembler implements RepresentationModelAssembler<MarketplaceChannelResponse, EntityModel<MarketplaceChannelResponse>> {

    private static final String BASE_URI = "/api/v1/channels";

    @Override
    public EntityModel<MarketplaceChannelResponse> toModel(MarketplaceChannelResponse response) {
        if (response == null) {
            return null;
        }

        EntityModel<MarketplaceChannelResponse> model = EntityModel.of(response);

        if (response.id() != null) {
            model.add(Link.of(BASE_URI + "/" + response.id()).withSelfRel());
            model.add(Link.of(BASE_URI + "/" + response.id() + "/status").withRel("toggle-status"));
        }

        model.add(Link.of(BASE_URI).withRel("collection"));

        return model;
    }

    @Override
    public CollectionModel<EntityModel<MarketplaceChannelResponse>> toCollectionModel(Iterable<? extends MarketplaceChannelResponse> entities) {
        List<EntityModel<MarketplaceChannelResponse>> models = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .toList();

        return CollectionModel.of(models, Link.of(BASE_URI).withSelfRel());
    }
}
