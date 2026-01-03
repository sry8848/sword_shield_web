package com.swordshield.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 文件服务启动类。
 *
 * <p>本服务提供对象存储（MinIO）文件的上传、下载预签名等能力，并维护文件元数据。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.swordshield.file", "com.swordshield.common"})
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}

