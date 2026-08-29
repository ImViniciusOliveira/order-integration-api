package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.response.MarketplaceChannelResponse;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper MapStruct responsável pela conversão entre a entidade MarketplaceChannel e o DTO de resposta.
 */
@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface MarketplaceChannelMapper {

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
}
