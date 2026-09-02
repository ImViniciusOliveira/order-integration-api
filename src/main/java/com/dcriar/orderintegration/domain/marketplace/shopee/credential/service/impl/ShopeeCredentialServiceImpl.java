package com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.impl;

import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.repository.ShopeeCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementação do acesso às credenciais Shopee persistidas no MongoDB.
 */
@Service
@RequiredArgsConstructor
public class ShopeeCredentialServiceImpl implements ShopeeCredentialService {

    private final ShopeeCredentialMongoRepository repository;

    @Override
    public ShopeeCredentialDocument getCredential(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalArgumentException("O shop_id da Shopee é obrigatório para consulta de credenciais");
        }

        return repository.findByShopId(shopId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("CredencialShopee", shopId));
    }
}
