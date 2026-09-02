package com.dcriar.orderintegration.domain.marketplace.shopee.credential.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Documento MongoDB com as credenciais e chaves de integração de uma loja Shopee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credenciais_lojas")
public class ShopeeCredentialDocument {

    @Id
    private String id;

    @Field("shop_id")
    private String shopId;

    @Field("plataforma")
    private String plataforma;

    @Field("partner_id")
    private String partnerId;

    @Field("live_partner_key")
    private String livePartnerKey;

    @Field("live_push_partner_key")
    private String livePushPartnerKey;

    @Field("live_access_token")
    private String liveAccessToken;

    @Field("live_refresh_token")
    private String liveRefreshToken;

    @Field("ultimo_update_data")
    private String ultimoUpdateData;

    @Field("vencimento_token_ts")
    private String vencimentoTokenTs;
}
