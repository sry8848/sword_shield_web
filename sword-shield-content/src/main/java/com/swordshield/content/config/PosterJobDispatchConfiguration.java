package com.swordshield.content.config;

import com.swordshield.content.mq.PosterJobDispatcher;
import com.swordshield.content.service.PosterJobProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;

@Configuration
@ConditionalOnProperty(prefix = "app.poster.mq", name = "enabled", havingValue = "false")
public class PosterJobDispatchConfiguration {
    @Bean
    public PosterJobDispatcher directPosterJobDispatcher(PosterJobProcessor processor) {
        return jobId -> CompletableFuture.runAsync(() -> processor.process(jobId));
    }
}
