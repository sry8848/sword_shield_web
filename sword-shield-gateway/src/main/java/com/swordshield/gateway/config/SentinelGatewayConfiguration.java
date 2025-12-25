package com.swordshield.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.swordshield.common.ErrorCode;
import com.swordshield.common.Response;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SentinelGatewayConfiguration {

    @PostConstruct
    public void initBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, ex) -> ServerResponse
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Response.error(ErrorCode.TOO_MANY_REQUESTS, "Too many requests, please try again later"));
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
