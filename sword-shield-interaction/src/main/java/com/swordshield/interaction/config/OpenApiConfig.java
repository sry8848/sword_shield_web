package com.swordshield.interaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI interactionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("互动服务 API")
                        .description("评论、点赞、分享等互动接口")
                        .version("1.0.0"));
    }
}
