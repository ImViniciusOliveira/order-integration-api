package com.dcriar.orderintegration.domain.credential.service.impl;

import com.dcriar.orderintegration.domain.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.credential.repository.MercadoLivreCredentialMongoRepository;
import com.dcriar.orderintegration.domain.credential.repository.ShopeeCredentialMongoRepository;
import com.dcriar.orderintegration.domain.credential.service.MarketplaceCredentialService;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementação estrita do serviço de credenciais das plataformas no MongoDB.
 * Caso o registro não seja encontrado no banco, lança ResourceNotFoundException imediatamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceCredentialServiceImpl implements MarketplaceCredentialService {

    private final ShopeeCredentialMongoRepository shopeeRepository;
    private final MercadoLivreCredentialMongoRepository mercadoLivreRepository;

    @Override
    public ShopeeCredentialDocument getShopeeCredential(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalArgumentException("O shop_id da Shopee é obrigatório para consulta de credenciais");
        }

        return shopeeRepository.findByShopId(shopId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("CredencialShopee", shopId));
    }

    @Override
    public MercadoLivreCredentialDocument getMercadoLivreCredential(String accountId) {
        if (accountId != null && !accountId.isBlank() && !"default".equalsIgnoreCase(accountId.trim())) {
            return mercadoLivreRepository.findByClientId(accountId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivreClientId", accountId));
        }

        return mercadoLivreRepository.findByPlataforma("mercadolivre")
                .orElseThrow(() -> new ResourceNotFoundException("CredencialMercadoLivre", "mercadolivre"));
    }
}
