package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper MapStruct responsável pela conversão entre a entidade de domínio OrderMaster e o DTO OrderMasterResponse.
 */
@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface OrderMasterMapper {

    /**
     * Converte a entidade de domínio OrderMaster em DTO de resposta.
     *
     * @param entity entidade persistida no PostgreSQL
     * @return DTO consolidado para resposta REST
     */
    OrderMasterResponse toResponse(OrderMaster entity);

    /**
     * Converte uma lista de entidades OrderMaster em uma lista de DTOs de resposta.
     *
     * @param entities lista de pedidos de domínio
     * @return lista de DTOs de resposta
     */
    List<OrderMasterResponse> toResponseList(List<OrderMaster> entities);
}
