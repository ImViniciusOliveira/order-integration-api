package com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.impl;

import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.repository.ShopeeCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementação do acesso às credenciais Shopee persistidas no MongoDB.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopeeCredentialServiceImpl implements ShopeeCredentialService {

    private final ShopeeCredentialMongoRepository repository;

    @Override
    public ShopeeCredentialDocument getCredential(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalArgumentException("O shop_id da Shopee é obrigatório para consulta de credenciais");
        }

        return repository.findByShopId(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("CredencialShopee", shopId));
    }

    @Override
    @Transactional
    public ShopeeCredentialDocument updateTokens(
            ShopeeCredentialDocument credential,
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
}
