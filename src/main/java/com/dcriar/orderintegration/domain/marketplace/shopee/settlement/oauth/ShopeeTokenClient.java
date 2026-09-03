package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.oauth;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import com.dcriar.orderintegration.domain.marketplace.shopee.settlement.signer.ShopeeRequestSigner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Client responsavel pela renovacao emergencial do access token da Shopee.
 */
@Component
public class ShopeeTokenClient {

    private static final long REFRESH_CONFLICT_RETRY_DELAY_SECONDS = 30L;

    private final ShopeeCredentialService credentialService;
    private final OrderIntegrationProperties.ShopeeProperties properties;
    private final ShopeeRequestSigner requestSigner;
    private final RestClient restClient;

    public ShopeeTokenClient(
            ShopeeCredentialService credentialService,
            OrderIntegrationProperties properties,
            ShopeeRequestSigner requestSigner,
            RestClient.Builder restClientBuilder
    ) {
        this.credentialService = credentialService;
        this.properties = properties.shopee();
        this.requestSigner = requestSigner;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Retorna a credencial atual ou renova o token quando ele estiver vencido.
     *
     * @param credential credencial armazenada atualmente
     * @return credencial valida
     */
    public ShopeeCredentialDocument ensureValid(ShopeeCredentialDocument credential) {
        if (!isExpired(credential)) {
            return credential;
        }
        validateRefreshCredential(credential);

        try {
            return renewAndPersist(credential);
        } catch (RestClientResponseException firstException) {
            sleepBeforeRetry();
            ShopeeCredentialDocument latestCredential = credentialService.getCredential(credential.getShopId());
            if (!isExpired(latestCredential)) {
                return latestCredential;
            }
            try {
                return renewAndPersist(latestCredential);
            } catch (RestClientResponseException retryException) {
                retryException.addSuppressed(firstException);
                throw retryException;
            }
        }
    }

    private ShopeeCredentialDocument renewAndPersist(ShopeeCredentialDocument credential) {
        long timestamp = Instant.now().getEpochSecond();
        String tokenPath = properties.tokenPath();
        String signature = requestSigner.signTokenRefresh(
                credential.getPartnerId(),
                tokenPath,
                timestamp,
                credential.getLivePartnerKey()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shop_id", parseLong(credential.getShopId()));
        body.put("refresh_token", credential.getLiveRefreshToken());
        body.put("partner_id", parseLong(credential.getPartnerId()));

        Map<String, Object> response = restClient.post()
                .uri(UriComponentsBuilder.fromUriString(properties.baseUrl())
                        .path(tokenPath)
                        .queryParam("partner_id", credential.getPartnerId())
                        .queryParam("timestamp", timestamp)
                        .queryParam("sign", signature)
                        .build()
                        .encode()
                        .toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String accessToken = stringValue(response, "access_token");
        String refreshToken = stringValue(response, "refresh_token");
        long expiresIn = longValue(response, "expire_in");
        if (accessToken == null || refreshToken == null || expiresIn <= 0) {
            throw new IllegalStateException("Resposta de renovacao da Shopee invalida");
        }

        return credentialService.updateTokens(
                credential,
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(expiresIn).getEpochSecond()
        );
    }

    private void sleepBeforeRetry() {
        try {
            TimeUnit.SECONDS.sleep(REFRESH_CONFLICT_RETRY_DELAY_SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry da renovacao da Shopee interrompido", exception);
        }
    }

    private boolean isExpired(ShopeeCredentialDocument credential) {
        if (credential == null || credential.getVencimentoTokenTs() == null
                || credential.getVencimentoTokenTs().isBlank()) {
            return true;
        }
        try {
            long expiration = Long.parseLong(credential.getVencimentoTokenTs());
            if (expiration > 100_000_000_000L) {
                expiration /= 1_000L;
            }
            return expiration <= Instant.now().getEpochSecond();
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private void validateRefreshCredential(ShopeeCredentialDocument credential) {
        if (credential == null
                || isBlank(credential.getPartnerId())
                || isBlank(credential.getLivePartnerKey())
                || isBlank(credential.getShopId())
                || isBlank(credential.getLiveRefreshToken())) {
            throw new IllegalStateException("Credencial Shopee incompleta para renovacao");
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Identificador numerico Shopee invalido", exception);
        }
    }

    private String stringValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value == null ? null : value.toString();
    }

    private long longValue(Map<String, Object> source, String key) {
        String value = stringValue(source, key);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
