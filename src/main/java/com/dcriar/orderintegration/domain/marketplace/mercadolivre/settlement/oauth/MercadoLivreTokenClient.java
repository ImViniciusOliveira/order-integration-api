package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.oauth;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Client OAuth2 responsável por verificar e renovar tokens do Mercado Livre.
 */
@Component
public class MercadoLivreTokenClient {

    private static final long REFRESH_CONFLICT_RETRY_DELAY_SECONDS = 30L;

    private final MercadoLivreCredentialService credentialService;
    private final OrderIntegrationProperties.MercadoLivreProperties properties;
    private final RestClient restClient;

    /**
     * Cria o client OAuth2 do Mercado Livre.
     *
     * @param credentialService serviço de persistência das credenciais
     * @param properties        propriedades da integração
     * @param restClientBuilder builder HTTP global
     */
    public MercadoLivreTokenClient(
            MercadoLivreCredentialService credentialService,
            OrderIntegrationProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.credentialService = credentialService;
        this.properties = properties.mercadoLivre();
        this.restClient = restClientBuilder.build();
    }

    /**
     * Garante um access token válido para a credencial informada.
     *
     * @param credential credencial atualmente armazenada
     * @return credencial original ou atualizada
     */
    public MercadoLivreCredentialDocument ensureValid(
            MercadoLivreCredentialDocument credential
    ) {
        if (!isExpired(credential)) {
            return credential;
        }
        validateRefreshCredential(credential);

        MercadoLivreCredentialDocument credentialToRefresh = credential;
        MercadoLivreTokenResponse response;
        try {
            response = requestToken(credentialToRefresh);
        } catch (RestClientResponseException firstException) {
            credentialToRefresh = reloadCredentialAfterRefreshFailure(credential, firstException);
            if (!isExpired(credentialToRefresh)) {
                return credentialToRefresh;
            }
            response = requestToken(credentialToRefresh);
        }
        String refreshToken = response.refreshToken() == null
                ? credentialToRefresh.getLiveRefreshToken()
                : response.refreshToken();
        long expirationEpoch = Instant.now().plusSeconds(response.expiresIn()).getEpochSecond();

        return credentialService.updateTokens(
                credentialToRefresh,
                response.accessToken(),
                refreshToken,
                expirationEpoch
        );
    }

    private MercadoLivreCredentialDocument reloadCredentialAfterRefreshFailure(
            MercadoLivreCredentialDocument credential,
            RestClientResponseException firstException
    ) {
        try {
            TimeUnit.SECONDS.sleep(REFRESH_CONFLICT_RETRY_DELAY_SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry da renovação OAuth2 do Mercado Livre interrompido",
                    interruptedException);
        }

        String accountId = !isBlank(credential.getSellerId())
                ? credential.getSellerId()
                : credential.getClientId();
        try {
            return credentialService.getCredential(accountId);
        } catch (RuntimeException reloadException) {
            reloadException.addSuppressed(firstException);
            throw reloadException;
        }
    }

    private MercadoLivreTokenResponse requestToken(
            MercadoLivreCredentialDocument credential
    ) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", credential.getClientId());
        form.add("client_secret", credential.getClientSecret());
        form.add("refresh_token", credential.getLiveRefreshToken());

        Map<String, Object> body = restClient.post()
                .uri(UriComponentsBuilder.fromUriString(properties.baseUrl())
                        .path(properties.oauthTokenPath())
                        .build()
                        .toUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String accessToken = stringValue(body, "access_token");
        long expiresIn = longValue(body, "expires_in");
        if (accessToken == null || expiresIn <= 0) {
            throw new IllegalStateException("Resposta OAuth2 do Mercado Livre inválida");
        }

        return new MercadoLivreTokenResponse(
                accessToken,
                stringValue(body, "refresh_token"),
                expiresIn
        );
    }

    private boolean isExpired(MercadoLivreCredentialDocument credential) {
        if (credential == null || credential.getVencimentoTokenTs() == null
                || credential.getVencimentoTokenTs().isBlank()) {
            return true;
        }
        try {
            long expiration = Long.parseLong(credential.getVencimentoTokenTs());
            long expirationEpochSeconds = expiration > 10_000_000_000L
                    ? expiration / 1000
                    : expiration;
            return expirationEpochSeconds <= Instant.now().getEpochSecond();
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private void validateRefreshCredential(MercadoLivreCredentialDocument credential) {
        if (credential == null
                || isBlank(credential.getClientId())
                || isBlank(credential.getClientSecret())
                || isBlank(credential.getLiveRefreshToken())) {
            throw new IllegalStateException("Credencial OAuth2 Mercado Livre incompleta para renovação");
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
