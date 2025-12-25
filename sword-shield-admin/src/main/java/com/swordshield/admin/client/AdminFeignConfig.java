package com.swordshield.admin.client;

import com.swordshield.common.TraceUtils;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminFeignConfig {
    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> TraceUtils.currentTraceId()
                .ifPresent(id -> template.header(TraceUtils.TRACE_ID_HEADER, id));
    }
}

