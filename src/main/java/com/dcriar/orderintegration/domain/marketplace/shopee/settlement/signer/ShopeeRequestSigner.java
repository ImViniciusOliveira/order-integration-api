package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.signer;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Gera assinaturas HMAC-SHA256 exigidas pela Shopee Open Platform.
 */
@Component
public class ShopeeRequestSigner {

    /**
     * Gera a assinatura da requisição conforme a composição oficial da Shopee.
     *
     * @param partnerId   identificador do parceiro
     * @param apiPath     caminho da API chamada
     * @param timestamp   timestamp Unix da requisição
     * @param accessToken token de acesso da loja
     * @param shopId      identificador da loja
     * @param partnerKey  chave privada do parceiro
     * @return assinatura hexadecimal HMAC-SHA256
     */
    public String sign(
            String partnerId,
            String apiPath,
            long timestamp,
            String accessToken,
            String shopId,
            String partnerKey
    ) {
        try {
            String baseString = partnerId + apiPath + timestamp + accessToken + shopId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                signature.append(String.format("%02x", value));
            }
            return signature.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a assinatura da Shopee", exception);
        }
    }

    /**
     * Gera a assinatura da renovacao de access token da Shopee.
     *
     * @param partnerId identificador do parceiro
     * @param apiPath caminho da rota de renovacao
     * @param timestamp timestamp Unix da requisicao
     * @param partnerKey chave privada do parceiro
     * @return assinatura hexadecimal HMAC-SHA256
     */
    public String signTokenRefresh(
            String partnerId,
            String apiPath,
            long timestamp,
            String partnerKey
    ) {
        try {
            String baseString = partnerId + apiPath + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                signature.append(String.format("%02x", value));
            }
            return signature.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel gerar a assinatura de renovacao da Shopee", exception);
        }
    }
}
