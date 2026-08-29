package com.dcriar.orderintegration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração centralizada da documentação OpenAPI 3 / Swagger UI.
 * <p>
 * Inclui esquemas de autenticação via cabeçalho {@code X-Internal-API-Key}
 * para permitir testes interativos dos endpoints de webhooks e gestão.
 */
@Configuration
public class OpenApiConfig {

    public static final String INTERNAL_API_KEY_SCHEME = "InternalApiKeyAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Integration API")
                        .version("1.0.0")
                        .description("Microsserviço de alta performance para ingestão resiliente de webhooks, "
                                + "cálculo contábil de taxas de marketplace e conciliação financeira automatizada.")
                        .contact(new Contact()
                                .name("DCriar Engenharia")
                                .url("https://dcriar.com")
                                .email("dev@dcriar.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://dcriar.com/license")))
                .components(new Components()
                        .addSecuritySchemes(INTERNAL_API_KEY_SCHEME, new SecurityScheme()
                                .name("X-Internal-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Chave de autenticação interna para disparo de webhooks de ingestão.")))
                .addSecurityItem(new SecurityRequirement().addList(INTERNAL_API_KEY_SCHEME));
    }
}
