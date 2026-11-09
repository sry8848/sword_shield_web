package com.swordshield.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan({"com.swordshield.admin.domain", "com.swordshield.activity.domain"})
@EnableJpaRepositories({"com.swordshield.admin.repository", "com.swordshield.activity.repository"})
@ComponentScan(basePackages = {"com.swordshield.admin", "com.swordshield.common"})
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
