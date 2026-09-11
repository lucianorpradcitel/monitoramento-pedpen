package com.citel.monitoramento_n8n.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor das sincronizações Shopify.
     *
     * Um único thread de propósito: cada execução expande ~9 mil categorias e envia uma a uma ao
     * ERP, levando dezenas de minutos. Duas em paralelo disputariam o mesmo webservice e criariam
     * as mesmas categorias duas vezes, já que o de-para de ids é local a cada execução.
     *
     * A fila existe só para não recusar um disparo que chegue enquanto a anterior termina; cheia,
     * o pedido é rejeitado e o controller responde 429 em vez de enfileirar trabalho indefinido.
     */
    @Bean("sincronizacaoExecutor")
    public Executor sincronizacaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(3);
        executor.setThreadNamePrefix("sync-shopify-");
        executor.initialize();

        return executor;
    }
}
