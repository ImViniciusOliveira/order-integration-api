package com.dcriar.orderintegration.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO imutável de requisição para cadastro e atualização de canais de marketplace.
 *
 * @param code   código identificador único do canal em maiúsculas (ex: "SHOPEE_OFICIAL", "MERCADO_LIVRE_01")
 * @param name   nome descritivo amigável para identificação da conta/loja
 * @param active indicador de habilitação da integração do canal
 */
public record MarketplaceChannelRequest(
        @NotBlank(message = "O código identificador do canal de marketplace é obrigatório.")
        @Size(min = 2, max = 30, message = "O código do canal deve conter entre 2 e 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "O código do canal deve conter apenas letras, números e sublinhados.")
        String code,

        @NotBlank(message = "O nome descritivo do canal é obrigatório.")
        @Size(min = 2, max = 100, message = "O nome do canal deve conter entre 2 e 100 caracteres.")
        String name,

        Boolean active
) {
    public MarketplaceChannelRequest {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (name != null) {
            name = name.trim();
        }
        if (active == null) {
            active = true;
        }
    }
}
