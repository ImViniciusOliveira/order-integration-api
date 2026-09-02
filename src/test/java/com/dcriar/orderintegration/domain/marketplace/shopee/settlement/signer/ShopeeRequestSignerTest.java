package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.signer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do gerador de assinatura Shopee.
 */
class ShopeeRequestSignerTest {

    @Test
    void deveGerarAssinaturaHexadecimalHmacSha256() {
        String signature = new ShopeeRequestSigner().sign(
                "partner",
                "/api/v2/payment/get_escrow_detail",
                1_700_000_000L,
                "access",
                "shop",
                "secret"
        );

        assertThat(signature).hasSize(64).matches("[0-9a-f]+");
    }
}
