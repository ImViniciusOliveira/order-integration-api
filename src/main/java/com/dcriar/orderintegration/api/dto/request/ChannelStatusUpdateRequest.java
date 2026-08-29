package com.dcriar.orderintegration.api.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO imutável de requisição para ativação ou desativação operacional de um canal de marketplace.
 *
 * @param active indicador de habilitação da integração (true para ativar, false para pausar)
 */
public record ChannelStatusUpdateRequest(
        @NotNull(message = "O indicador de status (active) é obrigatório.")
        Boolean active
) {
}
