package com.dcriar.orderintegration.api.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO imutável de resposta com os dados detalhados de um canal de marketplace cadastrado.
 *
 * @param id        identificador único do canal
 * @param code      código mnemônico padronizado (ex: "SHOPEE_OFICIAL")
 * @param name      nome legível do canal
 * @param active    indicador de canal ativo
 * @param createdAt data e hora de criação do registro
 * @param updatedAt data e hora da última modificação
 */
public record MarketplaceChannelResponse(
        Long id,
        String code,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
