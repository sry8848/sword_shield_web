package com.swordshield.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 互动服务启动类。
 *
 * <p>本服务负责评论、点赞、分享等互动能力，对不同目标对象（文章/活动等）以统一的 targetType/targetId 方式建模。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.swordshield.interaction", "com.swordshield.common"})
public class InteractionApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractionApplication.class, args);
    }
}

