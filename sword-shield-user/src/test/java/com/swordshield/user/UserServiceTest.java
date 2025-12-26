package com.swordshield.user;

import com.swordshield.common.JwtUtil;
import com.swordshield.user.domain.User;
import com.swordshield.user.repository.UserRepository;
import com.swordshield.user.service.LoginSecurityService;
import com.swordshield.user.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class UserServiceTest {
    @Test
    void testRegisterAndLogin() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        LoginSecurityService loginSecurityService = Mockito.mock(LoginSecurityService.class);
        Mockito.when(repo.findByUsername("alice")).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService service = new UserService(repo, loginSecurityService);
        service.register("alice", "p@ss", "p@ss");

        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("p@ss"));
        Mockito.when(repo.findByUsername("alice")).thenReturn(Optional.of(u));

        String token = service.login("alice", "p@ss", "127.0.0.1").getToken();
        String sub = JwtUtil.parseSubject(token);
        Assertions.assertEquals("1", sub);
    }
}

