package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.request.MarketplaceChannelRequest;
import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper MapStruct responsável pela conversão bidirecional entre DTOs e a entidade MarketplaceChannel.
 */
@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface MarketplaceChannelMapper {

    /**
     * Converte um DTO de requisição em uma nova entidade MarketplaceChannel.
     *
     * @param request dados da requisição
     * @return entidade preenchida
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MarketplaceChannel toEntity(MarketplaceChannelRequest request);

    /**
     * Converte a entidade MarketplaceChannel no DTO imutável de resposta.
     *
     * @param entity entidade do banco de dados
     * @return DTO de resposta
     */
    MarketplaceChannelResponse toResponse(MarketplaceChannel entity);

    /**
     * Converte uma lista de entidades MarketplaceChannel em uma lista de DTOs de resposta.
     *
     * @param entities lista de entidades
     * @return lista de DTOs de resposta
     */
    List<MarketplaceChannelResponse> toResponseList(List<MarketplaceChannel> entities);

    /**
     * Atualiza uma entidade existente a partir dos dados do DTO de requisição.
     *
     * @param request dados atualizados
     * @param entity  entidade de destino a ser modificada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(MarketplaceChannelRequest request, @MappingTarget MarketplaceChannel entity);
}
