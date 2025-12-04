package com.swordshield.content.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI contentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("内容服务 API")
                        .description("文章、展商、素材、媒体等内容接口")
                        .version("1.0.0"));
    }
}
