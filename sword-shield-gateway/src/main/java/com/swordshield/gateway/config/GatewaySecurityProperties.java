package com.swordshield.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    /**
     * Paths that should be accessible without authentication.
     * Uses Spring Security "pathMatchers" patterns.
     */
    private List<String> permitAll = new ArrayList<>();

    /**
     * Paths that require admin authorization.
     * Uses Spring Security "pathMatchers" patterns.
     */
    private List<String> adminPaths = new ArrayList<>();

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll == null ? new ArrayList<>() : permitAll;
    }

    public List<String> getAdminPaths() {
        return adminPaths;
    }

    public void setAdminPaths(List<String> adminPaths) {
        this.adminPaths = adminPaths == null ? new ArrayList<>() : adminPaths;
    }
}

