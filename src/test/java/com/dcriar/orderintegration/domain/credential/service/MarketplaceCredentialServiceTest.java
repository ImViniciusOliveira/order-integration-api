package com.dcriar.orderintegration.domain.credential.service;

import com.dcriar.orderintegration.domain.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.credential.repository.MercadoLivreCredentialMongoRepository;
import com.dcriar.orderintegration.domain.credential.repository.ShopeeCredentialMongoRepository;
import com.dcriar.orderintegration.domain.credential.service.impl.MarketplaceCredentialServiceImpl;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceCredentialServiceTest {

    @Mock
    private ShopeeCredentialMongoRepository shopeeRepository;

    @Mock
    private MercadoLivreCredentialMongoRepository mercadoLivreRepository;

    @InjectMocks
    private MarketplaceCredentialServiceImpl credentialService;

    @Test
    @DisplayName("Deve retornar credencial da Shopee com sucesso para shopId cadastrado")
    void deveRetornarCredencialShopeeComSucesso() {
        ShopeeCredentialDocument doc = ShopeeCredentialDocument.builder()
                .id("1")
                .shopId("326559200")
                .liveAccessToken("shopee-token-xyz")
                .build();

        when(shopeeRepository.findByShopId("326559200")).thenReturn(Optional.of(doc));

        ShopeeCredentialDocument resultado = credentialService.getShopeeCredential("326559200");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getLiveAccessToken()).isEqualTo("shopee-token-xyz");
        verify(shopeeRepository).findByShopId("326559200");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando shopId da Shopee não for encontrado")
    void deveLancarExcecaoQuandoShopeeNaoEncontrada() {
        when(shopeeRepository.findByShopId("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialService.getShopeeCredential("999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CredencialShopee")
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando shopId da Shopee for nulo ou vazio")
    void deveLancarExcecaoQuandoShopIdInvalido() {
        assertThatThrownBy(() -> credentialService.getShopeeCredential(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> credentialService.getShopeeCredential("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve retornar credencial do Mercado Livre por plataforma quando accountId for nulo ou default")
    void deveRetornarCredencialMercadoLivrePorPlataforma() {
        MercadoLivreCredentialDocument doc = MercadoLivreCredentialDocument.builder()
                .id("2")
                .plataforma("mercadolivre")
                .liveAccessToken("meli-token-abc")
                .build();

        when(mercadoLivreRepository.findByPlataforma("mercadolivre")).thenReturn(Optional.of(doc));

        MercadoLivreCredentialDocument resultado = credentialService.getMercadoLivreCredential(null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getLiveAccessToken()).isEqualTo("meli-token-abc");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando credencial do Mercado Livre não for encontrada")
    void deveLancarExcecaoQuandoMercadoLivreNaoEncontrado() {
        when(mercadoLivreRepository.findByPlataforma("mercadolivre")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialService.getMercadoLivreCredential("default"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CredencialMercadoLivre");
    }
}
