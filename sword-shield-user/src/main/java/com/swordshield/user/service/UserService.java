package com.swordshield.user.service;

import com.swordshield.common.JwtUtil;
import com.swordshield.user.domain.User;
import com.swordshield.user.dto.LoginResponse;
import com.swordshield.user.dto.UpdateUserProfileRequest;
import com.swordshield.user.dto.UserProfileDto;
import com.swordshield.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 用户账户应用服务。
 *
 * <p>负责注册、登录（签发 JWT）、以及用户资料查询/更新等用例。
 * 领域对象为 {@link com.swordshield.user.domain.User}，接口层 DTO 位于 {@code com.swordshield.user.dto}。
 */
@Service
public class UserService {
    private final UserRepository repo;
    private final LoginSecurityService loginSecurityService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo, LoginSecurityService loginSecurityService) {
        this.repo = repo;
        this.loginSecurityService = loginSecurityService;
    }

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

    public LoginResponse login(String username, String password, String clientIp) {
        loginSecurityService.beforeLogin(username, clientIp);

        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalArgumentException("用户名或密码错误");
            }

            User u = repo.findByUsername(username).orElseThrow(() ->
                new IllegalArgumentException("用户名或密码错误"));
            if (!encoder.matches(password, u.getPasswordHash())) {
                throw new IllegalArgumentException("用户名或密码错误");
            }

            String token = JwtUtil.generateToken(String.valueOf(u.getId()));
            LoginResponse res = new LoginResponse();
            res.setToken(token);
            res.setUserId(String.valueOf(u.getId()));

            loginSecurityService.onLoginSuccess(username);
            return res;
        } catch (IllegalArgumentException e) {
            loginSecurityService.onLoginFailure(username, clientIp);
            throw e;
        }
    }

    public UserProfileDto getProfile(Long id) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toProfileDto(u);
    }

    public UserProfileDto updateProfile(UpdateUserProfileRequest req) {
        if (req == null || req.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        User u = repo.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setNickname(req.getNickname());
        u.setGender(req.getGender());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setAvatarUrl(req.getAvatarUrl());
        u.setUpdatedAt(Instant.now());
        return toProfileDto(repo.save(u));
    }

    private UserProfileDto toProfileDto(User u) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setGender(u.getGender());
        dto.setPhone(u.getPhone());
        dto.setEmail(u.getEmail());
        dto.setAvatarUrl(u.getAvatarUrl());
        dto.setStatus(u.getStatus());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }
}
