package com.swordshield.next.identity.infrastructure;

import java.time.Duration;
import java.util.List;

import com.swordshield.next.identity.application.PortalAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisPortalLoginBudget implements PortalAuthentication.LoginBudget {

    private static final String PREFIX = "identity:portal:login:";

    // 第一次递增和设置 TTL 在同一脚本内完成，后续请求不会延长同一个窗口。
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

    /** 读取两个限额和固定窗口时长，由后端配置管理。 */
    RedisPortalLoginBudget(StringRedisTemplate redis, @Value("${identity.portal.login-phone-limit}") int phoneLimit,
            @Value("${identity.portal.login-ip-limit}") int ipLimit,
            @Value("${identity.portal.login-window}") Duration window) {
        this.redis = redis;
        this.phoneLimit = Integer.toString(phoneLimit);
        this.ipLimit = Integer.toString(ipLimit);
        this.windowMillis = Long.toString(window.toMillis());
    }

    /** 同时计入手机号及来源 IP，任一维度超限即拒绝。 */
    @Override
    public boolean allow(String phone, String sourceIp) {
        Long allowed = redis.execute(CHECK, List.of(PREFIX + "phone:" + phone, PREFIX + "ip:" + sourceIp),
                phoneLimit, ipLimit, windowMillis);
        return allowed == 1L;
    }
}
