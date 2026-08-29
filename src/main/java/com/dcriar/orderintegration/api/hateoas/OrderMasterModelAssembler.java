package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.api.mapper.OrderMasterMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Assembler HATEOAS responsável por converter a entidade JPA {@link OrderMaster}
 * no DTO {@link OrderMasterResponse} enriquecido com links de auto-navegação e paginação.
 */
@Component
@RequiredArgsConstructor
public class OrderMasterModelAssembler implements RepresentationModelAssembler<OrderMaster, EntityModel<OrderMasterResponse>> {

    private static final String BASE_URI = "/api/v1/orders";

    private final OrderMasterMapper mapper;

    @Override
    public EntityModel<OrderMasterResponse> toModel(OrderMaster entity) {
        if (entity == null) {
            return null;
        }

        OrderMasterResponse response = mapper.toResponse(entity);
        EntityModel<OrderMasterResponse> model = EntityModel.of(response);

        if (entity.getId() != null) {
            model.add(Link.of(BASE_URI + "/" + entity.getId()).withSelfRel());
        }

        model.add(Link.of(BASE_URI).withRel("collection"));
        model.add(Link.of("/api/v1/channels").withRel("channels"));

        return model;
    }

    @Override
    public CollectionModel<EntityModel<OrderMasterResponse>> toCollectionModel(Iterable<? extends OrderMaster> entities) {
        List<EntityModel<OrderMasterResponse>> models = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .toList();

        return CollectionModel.of(models, Link.of(BASE_URI).withSelfRel());
    }
}
