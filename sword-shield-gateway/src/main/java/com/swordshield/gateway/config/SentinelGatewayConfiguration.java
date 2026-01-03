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

/**
 * Sentinel网关配置类
 * 用于配置网关流量控制的阻塞处理器
 */
@Configuration
public class SentinelGatewayConfiguration {

    /**
     * 初始化Sentinel网关阻塞处理器
     * 在Spring容器初始化完成后设置自定义的阻塞请求处理器
     */
    @PostConstruct
    public void initBlockHandler() {
        // 创建自定义的阻塞请求处理器，当请求被限流时返回429状态码和错误信息
        BlockRequestHandler blockRequestHandler = (exchange, ex) -> ServerResponse
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Response.error(ErrorCode.TOO_MANY_REQUESTS, "Too many requests, please try again later"));
        // 设置全局的阻塞处理器
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
