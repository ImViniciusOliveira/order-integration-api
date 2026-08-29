package com.dcriar.orderintegration.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO imutável de requisição para ativação ou desativação operacional de um canal de marketplace.
 *
 * @param active indicador de habilitação da integração (true para ativar, false para pausar)
 */
@Schema(description = "Payload para atualização do status de ativação de um canal de venda")
public record ChannelStatusUpdateRequest(
        @Schema(description = "Indicador de habilitação da integração", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O indicador de status (active) é obrigatório.")
        Boolean active
) {
}
