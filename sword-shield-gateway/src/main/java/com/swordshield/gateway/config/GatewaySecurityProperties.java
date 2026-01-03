package com.swordshield.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置属性类
 * 用于配置网关的安全相关属性，包括无需身份验证的路径和需要管理员授权的路径
 */
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    /**
     * 不需要身份验证即可访问的路径。
     * 使用 Spring Security "pathMatchers" 模式。
     */
    private List<String> permitAll = new ArrayList<>();

    /**
     * 需要管理员授权的路径。
     * 使用 Spring Security "pathMatchers" 模式。
     */
    private List<String> adminPaths = new ArrayList<>();

    /**
     * 获取无需身份验证即可访问的路径列表
     * @return 无需身份验证的路径列表
     */
    public List<String> getPermitAll() {
        return permitAll;
    }

    /**
     * 设置无需身份验证即可访问的路径列表
     * @param permitAll 无需身份验证的路径列表，如果为null则初始化为空列表
     */
    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll == null ? new ArrayList<>() : permitAll;
    }

    /**
     * 获取需要管理员授权的路径列表
     * @return 需要管理员授权的路径列表
     */
    public List<String> getAdminPaths() {
        return adminPaths;
    }

    /**
     * 设置需要管理员授权的路径列表
     * @param adminPaths 需要管理员授权的路径列表，如果为null则初始化为空列表
     */
    public void setAdminPaths(List<String> adminPaths) {
        this.adminPaths = adminPaths == null ? new ArrayList<>() : adminPaths;
    }
}

