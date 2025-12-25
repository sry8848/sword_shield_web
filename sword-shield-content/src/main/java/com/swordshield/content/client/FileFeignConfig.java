package com.swordshield.content.client;

import feign.codec.Encoder;
import feign.RequestInterceptor;
import com.swordshield.common.TraceUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileFeignConfig {
    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> {
            TraceUtils.currentTraceParent().ifPresent(tp -> template.header(TraceUtils.TRACEPARENT_HEADER, tp));
            TraceUtils.currentTraceId().ifPresent(id -> template.header(TraceUtils.TRACE_ID_HEADER, id));
        };
    }
}
