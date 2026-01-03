package com.swordshield.admin.service;

import com.swordshield.admin.domain.AdminUser;
import com.swordshield.admin.repository.AdminUserRepository;
import com.swordshield.common.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * 后台管理员鉴权服务。
 *
 * <p>负责管理员账号密码校验、状态检查，并签发管理端使用的 JWT。
 */
@Service
public class AdminAuthService {
    private final AdminUserRepository adminUserRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminAuthService(AdminUserRepository adminUserRepo) {
        this.adminUserRepo = adminUserRepo;
    }

    public Map<String, String> login(String username, String password) {
        AdminUser admin = adminUserRepo.findByUsername(username).orElseThrow(() -> 
            new IllegalArgumentException("管理员用户名或密码错误"));
        
        if (!encoder.matches(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("管理员用户名或密码错误");
        }
        
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new IllegalArgumentException("管理员账号已被禁用");
        }
        
        String token = JwtUtil.generateToken("admin_" + admin.getId());
        return Map.of(
            "token", token,
            "adminId", String.valueOf(admin.getId()),
            "username", admin.getUsername(),
            "nickname", admin.getNickname() != null ? admin.getNickname() : admin.getUsername()
        );
    }
}
