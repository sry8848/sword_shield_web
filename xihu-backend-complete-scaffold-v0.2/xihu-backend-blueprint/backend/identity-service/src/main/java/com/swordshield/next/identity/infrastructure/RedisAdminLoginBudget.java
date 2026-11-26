package com.swordshield.next.identity.infrastructure;

import java.time.Duration;
import java.util.List;

import com.swordshield.next.identity.application.AdminAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisAdminLoginBudget implements AdminAuthentication.LoginBudget {

    static final String PREFIX = "identity:admin:login:";

    private static final RedisScript<Long> CHECK = RedisScript.of("""
            local phone = redis.call('INCR', KEYS[1])
            if phone == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[3]) end
            local ip = redis.call('INCR', KEYS[2])
            if ip == 1 then redis.call('PEXPIRE', KEYS[2], ARGV[3]) end
            if phone <= tonumber(ARGV[1]) and ip <= tonumber(ARGV[2]) then return 1 end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String phoneLimit;
    private final String ipLimit;
    private final String windowMillis;

    RedisAdminLoginBudget(StringRedisTemplate redis,
            @Value("${identity.admin.login-phone-limit}") int phoneLimit,
            @Value("${identity.admin.login-ip-limit}") int ipLimit,
            @Value("${identity.admin.login-window}") Duration window) {
        this.redis = redis;
        this.phoneLimit = Integer.toString(phoneLimit);
        this.ipLimit = Integer.toString(ipLimit);
        this.windowMillis = Long.toString(window.toMillis());
    }

    @Override
    public boolean allow(String phone, String sourceIp) {
        Long allowed = redis.execute(CHECK, List.of(PREFIX + "phone:" + phone, PREFIX + "ip:" + sourceIp),
                phoneLimit, ipLimit, windowMillis);
        return allowed == 1L;
    }
}
