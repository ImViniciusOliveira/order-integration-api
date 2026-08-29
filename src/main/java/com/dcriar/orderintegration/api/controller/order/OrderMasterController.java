package com.dcriar.orderintegration.api.controller.order;

import com.dcriar.orderintegration.api.dto.filter.OrderFilterRequest;
import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.api.hateoas.OrderMasterModelAssembler;
import com.dcriar.orderintegration.api.mapper.OrderMasterMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import com.dcriar.orderintegration.domain.order.service.OrderMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consulta paginada, filtros dinâmicos e detalhes de pedidos mestre integrados.
 */
@Tag(name = "Orders Master", description = "Consulta paginada, detalhamento consolidado e filtros dinâmicos de pedidos")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderMasterController {

    private final OrderMasterService orderMasterService;
    private final OrderMasterMapper orderMasterMapper;
    private final OrderMasterModelAssembler orderMasterModelAssembler;
    private final PagedResourcesAssembler<OrderMaster> pagedResourcesAssembler;

    /**
     * Realiza a busca paginada e dinâmica de pedidos integrados com suporte a múltiplos filtros.
     *
     * @param filter   filtros de busca fornecidos na query string
     * @param pageable parâmetros de paginação e ordenação
     * @return modelo HATEOAS paginado contendo os pedidos filtrados
     */
    @Operation(summary = "Pesquisar pedidos com paginação e múltiplos filtros dinâmicos")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OrderMasterResponse>>> searchOrders(
            @ParameterObject @ModelAttribute OrderFilterRequest filter,
            @ParameterObject Pageable pageable) {
        OrderFilterCriteria criteria = orderMasterMapper.toCriteria(filter);
        Page<OrderMaster> ordersPage = orderMasterService.searchOrders(criteria, pageable);
        PagedModel<EntityModel<OrderMasterResponse>> pagedModel = pagedResourcesAssembler.toModel(ordersPage, orderMasterModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Consulta os detalhes consolidados de um pedido mestre pelo seu identificador único.
     *
     * @param id identificador único do pedido
     * @return representação HATEOAS do pedido consultado
     */
    @Operation(summary = "Buscar pedido consolidado por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<OrderMasterResponse>> findById(
            @Parameter(description = "Identificador único do pedido", example = "1")
            @PathVariable Long id) {
        OrderMaster order = orderMasterService.findById(id);
        return ResponseEntity.ok(orderMasterModelAssembler.toModel(order));
    }
}
