package com.dcriar.orderintegration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriedades fortemente tipadas de configuração da aplicação carregadas do {@code application.yaml}
 * ou sobrescritas por variáveis de ambiente.
 *
 * @param redis    configurações de chave e infraestrutura do Redis
 * @param escrow   configurações de conciliação financeira de Escrow
 * @param security configurações de segurança e chaves de autenticação interna
 */
@ConfigurationProperties(prefix = "order-integration")
public record OrderIntegrationProperties(
        @DefaultValue RedisProperties redis,
        @DefaultValue EscrowProperties escrow,
        @DefaultValue SecurityProperties security
) {

    /**
     * Configurações específicas para operações e filas no Redis.
     *
     * @param escrowQueueKey nome da chave do Sorted Set (ZSet) para fila de atraso de conciliação
     */
    public record RedisProperties(
            @DefaultValue("dcriar:orders:escrow_delay_queue") String escrowQueueKey
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
            @DefaultValue("120") int delayMinutes,
            @DefaultValue("30") int retryDelayMinutes,
            @DefaultValue("60000") long workerIntervalMs,
            @DefaultValue("50") int batchSize,
            @DefaultValue("5") int maxRetries
    ) {
    }

    /**
     * Configurações de segurança e autenticação interna para webhooks e integrações.
     *
     * @param internalApiKey chave de autenticação estática exigida no cabeçalho X-Internal-API-Key
     */
    public record SecurityProperties(
            @DefaultValue("dev-internal-key-12345") String internalApiKey
    ) {
    }
}
