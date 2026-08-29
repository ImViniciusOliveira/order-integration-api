package com.dcriar.orderintegration.api.dto.request;

import com.dcriar.orderintegration.api.validation.ValidChannelCode;
import com.dcriar.orderintegration.api.validation.ValidChannelName;

/**
 * DTO imutável de requisição para cadastro e atualização de canais de marketplace.
 *
 * @param code   código identificador único do canal em maiúsculas (ex: "SHOPEE_OFICIAL", "MERCADO_LIVRE_01")
 * @param name   nome descritivo amigável para identificação da conta/loja
 * @param active indicador de habilitação da integração do canal
 */
public record MarketplaceChannelRequest(
        @ValidChannelCode
        String code,

        @ValidChannelName
        String name,

        Boolean active
) {
}
