package com.swordshield.file.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI fileOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("文件服务 API")
                        .description("文件上传、下载与预签名接口")
                        .version("1.0.0"));
    }
}
