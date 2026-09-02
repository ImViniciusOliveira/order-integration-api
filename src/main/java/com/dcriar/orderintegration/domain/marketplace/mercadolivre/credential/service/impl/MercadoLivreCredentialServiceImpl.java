package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.impl;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.repository.MercadoLivreCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            return repository.findByClientId(accountId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivreClientId", accountId));
        }

        return repository.findByPlataforma(PLATFORM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivre", PLATFORM_CODE));
    }

    private boolean isDefaultAccount(String accountId) {
        return "default".equalsIgnoreCase(accountId.trim());
    }
}
