package com.swordshield.user.service;

import com.swordshield.common.JwtUtil;
import com.swordshield.user.domain.User;
import com.swordshield.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
public class UserService {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) { this.repo = repo; }

    public void register(String username, String password, String confirmPassword) {
        if (username == null || password == null || confirmPassword == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        repo.findByUsername(username).ifPresent(u -> { 
            throw new IllegalArgumentException("用户名已存在，请换一个用户名"); 
        });
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setAvatarUrl("/static/default-avatar.png");
        u.setStatus(1);  // 1 = 正常状态
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        repo.save(u);
    }

    public Map<String, String> login(String username, String password) {
        User u = repo.findByUsername(username).orElseThrow(() -> 
            new IllegalArgumentException("用户名或密码错误"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = JwtUtil.generateToken(String.valueOf(u.getId()));
        return Map.of(
            "token", token,
            "userId", String.valueOf(u.getId())
        );
    }
}

