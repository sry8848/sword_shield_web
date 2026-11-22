package com.swordshield.next.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class IdentityPasswordConfiguration {

    /** 创建供服务验密和离线预置共同使用的 Argon2id 编码器。 */
    @Bean
    static Argon2PasswordEncoder identityPasswordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }
}
