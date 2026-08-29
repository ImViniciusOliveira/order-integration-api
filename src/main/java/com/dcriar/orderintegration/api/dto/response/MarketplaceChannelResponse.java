package com.dcriar.orderintegration.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Dados consolidados de um canal de venda de marketplace")
public record MarketplaceChannelResponse(
        @Schema(description = "Identificador único do canal", example = "1")
        Long id,

        @Schema(description = "Código mnemônico padronizado do canal", example = "SHOPEE")
        String code,

        @Schema(description = "Nome legível do canal", example = "Shopee")
        String name,

        @Schema(description = "Indicador de canal ativo para ingestão de eventos", example = "true")
        boolean active,

        @Schema(description = "Data e hora de criação do registro", example = "2026-08-29T10:44:26.273835Z")
        OffsetDateTime createdAt,

        @Schema(description = "Data e hora da última modificação", example = "2026-08-29T10:44:26.273835Z")
        OffsetDateTime updatedAt
) {
}
