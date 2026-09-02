package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.repository.MercadoLivreCredentialMongoRepository;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.impl.MercadoLivreCredentialServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de credenciais Mercado Livre.
 */
@ExtendWith(MockitoExtension.class)
class MercadoLivreCredentialServiceTest {

    @Mock
    private MercadoLivreCredentialMongoRepository repository;

    @InjectMocks
    private MercadoLivreCredentialServiceImpl service;

    @Test
    void deveBuscarCredencialPorClientId() {
        MercadoLivreCredentialDocument credential = MercadoLivreCredentialDocument.builder()
                .clientId("app-123")
                .liveAccessToken("token")
                .build();
        when(repository.findByClientId("app-123")).thenReturn(Optional.of(credential));

        assertThat(service.getCredential(" app-123 ")).isSameAs(credential);
    }

    @Test
    void deveBuscarCredencialPadrao() {
        MercadoLivreCredentialDocument credential = MercadoLivreCredentialDocument.builder()
                .plataforma("mercadolivre")
                .liveAccessToken("token")
                .build();
        when(repository.findByPlataforma("mercadolivre")).thenReturn(Optional.of(credential));

        assertThat(service.getCredential(null)).isSameAs(credential);
    }

    @Test
    void deveBuscarCredencialPorSellerId() {
        MercadoLivreCredentialDocument credential = MercadoLivreCredentialDocument.builder()
                .sellerId("seller-123")
                .liveAccessToken("token")
                .build();
        when(repository.findBySellerId("seller-123")).thenReturn(Optional.of(credential));

        assertThat(service.getCredential("seller-123")).isSameAs(credential);
        verify(repository, never()).findByClientId("seller-123");
    }
}
