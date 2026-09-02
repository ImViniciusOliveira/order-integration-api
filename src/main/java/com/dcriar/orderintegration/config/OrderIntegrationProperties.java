package com.dcriar.orderintegration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propriedades fortemente tipadas de configuração da aplicação carregadas do {@code application.yaml}
 * ou sobrescritas por variáveis de ambiente.
 *
 * @param redis        configurações de chave e infraestrutura do Redis
 * @param escrow       configurações de conciliação financeira de Escrow
 * @param security     configurações de segurança e chaves de autenticação interna
 * @param cors         configurações de origens permitidas para requisições cross-origin (CORS)
 * @param notification configurações de webhook e integração de notificações externas (ex: n8n)
 * @param shopee       configurações da API financeira da Shopee
 */
@ConfigurationProperties(prefix = "order-integration")
public record OrderIntegrationProperties(
        RedisProperties redis,
        EscrowProperties escrow,
        SecurityProperties security,
        CorsProperties cors,
        NotificationProperties notification,
        ShopeeProperties shopee
) {

    /**
     * Configurações específicas para operações e filas no Redis.
     *
     * @param escrowQueueKey nome da chave do Sorted Set (ZSet) para fila de atraso de conciliação
     */
    public record RedisProperties(
            String escrowQueueKey
    ) {
    }

    /**
     * Configurações de tempo, lotes e regras de conciliação de Escrow.
     *
     * @param delayMinutes      tempo em minutos de espera inicial após o status COMPLETED antes de consultar a API de Escrow
     * @param retryDelayMinutes tempo em minutos de espera adicional para nova tentativa caso o extrato não esteja liberado
     * @param workerIntervalMs  intervalo em milissegundos entre as execuções do worker de conciliação
     * @param batchSize         quantidade máxima de pedidos resgatados da fila por execução
     * @param maxRetries        número máximo de reagendamentos permitidos antes de descarte/alerta
     */
    public record EscrowProperties(
            int delayMinutes,
            int retryDelayMinutes,
            long workerIntervalMs,
            int batchSize,
            int maxRetries
    ) {
    }

    /**
     * Configurações de segurança e autenticação interna para webhooks e integrações.
     *
     * @param internalApiKey chave de autenticação estática exigida no cabeçalho X-Internal-API-Key
     */
    public record SecurityProperties(
            String internalApiKey
    ) {
    }

    /**
     * Configurações de origens permitidas para comunicação Cross-Origin (CORS).
     *
     * @param allowedOrigins lista de domínios ou padrões de origens autorizadas a consumir a API REST
     */
    public record CorsProperties(
            List<String> allowedOrigins
    ) {
    }

    /**
     * Configurações de integração para disparo de notificações externas.
     *
     * @param n8nReconciliationWebhookUrl URL de webhook do n8n para notificação de conciliação financeira
     */
    public record NotificationProperties(
            String n8nReconciliationWebhookUrl
    ) {
    }

    /**
     * Configurações HTTP da API de settlement da Shopee.
     *
     * @param baseUrl    URL base da Shopee Open Platform
     * @param escrowPath path do endpoint de detalhes de escrow
     */
    public record ShopeeProperties(
            String baseUrl,
            String escrowPath
    ) {
    }
}
