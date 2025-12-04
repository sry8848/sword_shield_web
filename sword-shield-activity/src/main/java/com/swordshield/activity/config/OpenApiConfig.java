package com.swordshield.activity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI activityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("活动服务 API")
                        .description("活动列表、报名、统计等接口")
                        .version("1.0.0"));
    }
}
