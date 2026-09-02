package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.impl;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.repository.MercadoLivreCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Implementação do acesso às credenciais Mercado Livre persistidas no MongoDB.
 */
@Service
@RequiredArgsConstructor
public class MercadoLivreCredentialServiceImpl implements MercadoLivreCredentialService {

    private static final String PLATFORM_CODE = "mercadolivre";

    private final MercadoLivreCredentialMongoRepository repository;

    @Override
    public MercadoLivreCredentialDocument getCredential(String accountId) {
        if (accountId != null && !accountId.isBlank() && !isDefaultAccount(accountId)) {
            String normalizedAccountId = accountId.trim();
            return repository.findBySellerId(normalizedAccountId)
                    .or(() -> repository.findByClientId(normalizedAccountId))
                    .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivreConta", accountId));
        }

        return repository.findByPlataforma(PLATFORM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivre", PLATFORM_CODE));
    }

    @Override
    public MercadoLivreCredentialDocument updateTokens(
            MercadoLivreCredentialDocument credential,
            String accessToken,
            String refreshToken,
            long expirationEpoch
    ) {
        credential.setLiveAccessToken(accessToken);
        credential.setLiveRefreshToken(refreshToken);
        credential.setVencimentoTokenTs(Long.toString(expirationEpoch));
        credential.setUltimoUpdateData(Instant.now().toString());
        return repository.save(credential);
    }

    private boolean isDefaultAccount(String accountId) {
        return "default".equalsIgnoreCase(accountId.trim());
    }
}
