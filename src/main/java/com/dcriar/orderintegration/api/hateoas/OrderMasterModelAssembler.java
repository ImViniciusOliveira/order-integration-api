package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Assembler HATEOAS responsável por transformar {@link OrderMasterResponse} em {@link EntityModel}
 * contendo links de auto-navegação para endpoints de consulta e coleções paginadas.
 */
@Component
public class OrderMasterModelAssembler implements RepresentationModelAssembler<OrderMasterResponse, EntityModel<OrderMasterResponse>> {

    private static final String BASE_URI = "/api/v1/orders";

    @Override
    public EntityModel<OrderMasterResponse> toModel(OrderMasterResponse response) {
        if (response == null) {
            return null;
        }

        EntityModel<OrderMasterResponse> model = EntityModel.of(response);

        if (response.id() != null) {
            model.add(Link.of(BASE_URI + "/" + response.id()).withSelfRel());
        }

        model.add(Link.of(BASE_URI).withRel("collection"));
        model.add(Link.of("/api/v1/channels").withRel("channels"));

        return model;
    }

    @Override
    public CollectionModel<EntityModel<OrderMasterResponse>> toCollectionModel(Iterable<? extends OrderMasterResponse> entities) {
        List<EntityModel<OrderMasterResponse>> models = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .toList();

        return CollectionModel.of(models, Link.of(BASE_URI).withSelfRel());
    }
}
