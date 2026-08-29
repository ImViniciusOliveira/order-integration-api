package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.controller.channel.MarketplaceChannelController;
import com.dcriar.orderintegration.api.controller.order.OrderMasterController;
import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.api.mapper.OrderMasterMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler HATEOAS responsável por converter a entidade JPA {@link OrderMaster}
 * no DTO {@link OrderMasterResponse} enriquecido com links de auto-navegação e paginação type-safe.
 */
@Component
@RequiredArgsConstructor
public class OrderMasterModelAssembler implements RepresentationModelAssembler<OrderMaster, EntityModel<OrderMasterResponse>> {

    private final OrderMasterMapper mapper;

    @Override
    public EntityModel<OrderMasterResponse> toModel(OrderMaster entity) {
        if (entity == null) {
            return null;
        }

        OrderMasterResponse response = mapper.toResponse(entity);
        EntityModel<OrderMasterResponse> model = EntityModel.of(response);

        if (entity.getId() != null) {
            model.add(linkTo(methodOn(OrderMasterController.class).findById(entity.getId())).withSelfRel());
        }

        model.add(linkTo(methodOn(OrderMasterController.class).searchOrders(null, null)).withRel("collection"));
        model.add(linkTo(methodOn(MarketplaceChannelController.class).listAll()).withRel("channels"));

        return model;
    }

    @Override
    public CollectionModel<EntityModel<OrderMasterResponse>> toCollectionModel(Iterable<? extends OrderMaster> entities) {
        List<EntityModel<OrderMasterResponse>> models = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .toList();

        return CollectionModel.of(models, linkTo(methodOn(OrderMasterController.class).searchOrders(null, null)).withSelfRel());
    }
}
