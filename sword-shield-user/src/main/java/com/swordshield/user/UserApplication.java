package com.swordshield.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 用户服务启动类。
 *
 * <p>本服务提供用户注册登录、资料维护、积分与签到等能力：
 * <ul>
 *   <li>{@code com.swordshield.user.domain}：用户/积分等领域模型（JPA Entity）。</li>
 *   <li>{@code com.swordshield.user.dto}：HTTP 接口层 DTO（请求/响应）。</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.swordshield.user", "com.swordshield.common"})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}

