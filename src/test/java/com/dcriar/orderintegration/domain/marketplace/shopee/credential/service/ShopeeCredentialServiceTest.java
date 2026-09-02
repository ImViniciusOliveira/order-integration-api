package com.dcriar.orderintegration.domain.marketplace.shopee.credential.service;

import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.repository.ShopeeCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.impl.ShopeeCredentialServiceImpl;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de credenciais Shopee.
 */
@ExtendWith(MockitoExtension.class)
class ShopeeCredentialServiceTest {

    @Mock
    private ShopeeCredentialMongoRepository repository;

    @InjectMocks
    private ShopeeCredentialServiceImpl service;

    @Test
    void deveBuscarCredencialPorShopId() {
        ShopeeCredentialDocument credential = ShopeeCredentialDocument.builder()
                .shopId("326559200")
                .liveAccessToken("token")
                .build();
        when(repository.findByShopId("326559200")).thenReturn(Optional.of(credential));

        assertThat(service.getCredential("326559200")).isSameAs(credential);
    }

    @Test
    void deveFalharQuandoCredencialNaoExistir() {
        when(repository.findByShopId("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCredential("999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
