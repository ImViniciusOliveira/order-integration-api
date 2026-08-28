package com.dcriar.orderintegration.config;

import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Configuração global responsável por travar o fuso horário padrão da JVM
 * da aplicação para {@code America/Sao_Paulo}, evitando discrepâncias de horário.
 */
@Configuration
public class ApplicationTimeZoneConfig {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
}
