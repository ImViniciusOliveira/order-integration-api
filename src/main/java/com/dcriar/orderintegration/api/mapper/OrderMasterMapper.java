package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.filter.OrderFilterRequest;
import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper MapStruct responsável pela conversão entre a entidade de domínio OrderMaster,
 * DTOs de resposta e mapeamento de critérios de filtro.
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

    /**
     * Converte o DTO de filtro de requisição HTTP para o modelo de critérios de busca do domínio.
     *
     * @param request DTO de filtro recebido na rota REST
     * @return critérios tipados para o mecanismo de JPA Specifications
     */
    OrderFilterCriteria toCriteria(OrderFilterRequest request);
}
