package com.swordshield.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 活动服务启动类。
 *
 * <p>本服务负责活动内容的发布/展示、报名与统计等能力：
 * <ul>
 *   <li>{@code com.swordshield.activity.domain}：活动领域模型（JPA Entity）。</li>
 *   <li>{@code com.swordshield.activity.dto}：HTTP 接口层 DTO（请求/响应）。</li>
 *   <li>{@code com.swordshield.api.activity}：跨服务（Dubbo/RPC）传输的契约 DTO。</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EntityScan("com.swordshield.activity.domain")
@EnableJpaRepositories("com.swordshield.activity.repository")
@ComponentScan(basePackages = {"com.swordshield.activity", "com.swordshield.common"})
public class ActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}

