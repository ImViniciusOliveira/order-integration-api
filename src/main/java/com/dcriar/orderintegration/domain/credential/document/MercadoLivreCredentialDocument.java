package com.dcriar.orderintegration.domain.credential.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Documento de credenciais e chaves OAuth2 da conta Mercado Livre / Mercado Pago persistidas no MongoDB.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credenciais_lojas")
public class MercadoLivreCredentialDocument {

    @Id
    private String id;

    @Field("plataforma")
    private String plataforma;

    @Field("client_id")
    private String clientId;

    @Field("client_secret")
    private String clientSecret;

    @Field("live_access_token")
    private String liveAccessToken;

    @Field("live_refresh_token")
    private String liveRefreshToken;

    @Field("ultimo_update_data")
    private String ultimoUpdateData;

    @Field("vencimento_token_ts")
    private String vencimentoTokenTs;
}
