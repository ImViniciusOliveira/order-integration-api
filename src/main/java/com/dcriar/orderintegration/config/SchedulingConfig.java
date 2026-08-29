package com.dcriar.orderintegration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração global para habilitar e gerenciar a execução de tarefas agendadas em segundo plano
 * (Workers, rotinas de conciliação de Escrow e limpezas automáticas) via Spring Task Scheduling.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
