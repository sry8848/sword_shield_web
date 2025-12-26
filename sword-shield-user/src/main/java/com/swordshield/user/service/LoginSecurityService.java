package com.swordshield.user.service;

import com.swordshield.common.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class LoginSecurityService {
    private static final Logger log = LoggerFactory.getLogger(LoginSecurityService.class);

    private static final String KEY_PREFIX = "ss:login:";

    private final StringRedisTemplate redis;

    private final int ipPerMinuteLimit;
    private final int userPerMinuteLimit;

    private final int userMaxFailures;
    private final Duration userFailureWindow;
    private final Duration userLockDuration;

    private final int ipMaxFailures;
    private final Duration ipFailureWindow;
    private final Duration ipLockDuration;

    public LoginSecurityService(
            StringRedisTemplate redis,
            @Value("${app.security.login.rate-limit.ip-per-minute:60}") int ipPerMinuteLimit,
            @Value("${app.security.login.rate-limit.user-per-minute:20}") int userPerMinuteLimit,
            @Value("${app.security.login.lock.user-max-failures:5}") int userMaxFailures,
            @Value("${app.security.login.lock.user-window-seconds:900}") long userWindowSeconds,
            @Value("${app.security.login.lock.user-lock-seconds:900}") long userLockSeconds,
            @Value("${app.security.login.lock.ip-max-failures:30}") int ipMaxFailures,
            @Value("${app.security.login.lock.ip-window-seconds:900}") long ipWindowSeconds,
            @Value("${app.security.login.lock.ip-lock-seconds:900}") long ipLockSeconds) {
        this.redis = redis;
        this.ipPerMinuteLimit = Math.max(0, ipPerMinuteLimit);
        this.userPerMinuteLimit = Math.max(0, userPerMinuteLimit);
        this.userMaxFailures = Math.max(1, userMaxFailures);
        this.userFailureWindow = Duration.ofSeconds(Math.max(1, userWindowSeconds));
        this.userLockDuration = Duration.ofSeconds(Math.max(1, userLockSeconds));
        this.ipMaxFailures = Math.max(1, ipMaxFailures);
        this.ipFailureWindow = Duration.ofSeconds(Math.max(1, ipWindowSeconds));
        this.ipLockDuration = Duration.ofSeconds(Math.max(1, ipLockSeconds));
    }

    public void beforeLogin(String username, String clientIp) {
        String safeIp = normalizeIp(clientIp);
        String safeUser = normalizeUser(username);

        try {
            // Lock first (fast-fail)
            if (StringUtils.hasText(safeUser)) {
                String userLockKey = lockUserKey(safeUser);
                if (isLocked(userLockKey)) {
                    throw new TooManyRequestsException(lockMessage(getTtlSeconds(userLockKey)));
                }
            }
            String ipLockKey = lockIpKey(safeIp);
            if (isLocked(ipLockKey)) {
                throw new TooManyRequestsException(lockMessage(getTtlSeconds(ipLockKey)));
            }

            // Rate limit
            if (ipPerMinuteLimit > 0) {
                long ipCount = incrementWithExpire(rateIpKey(safeIp), Duration.ofMinutes(1));
                if (ipCount > ipPerMinuteLimit) {
                    throw new TooManyRequestsException("登录请求过于频繁，请稍后再试");
                }
            }

            if (StringUtils.hasText(safeUser) && userPerMinuteLimit > 0) {
                long userCount = incrementWithExpire(rateUserKey(safeUser), Duration.ofMinutes(1));
                if (userCount > userPerMinuteLimit) {
                    throw new TooManyRequestsException("登录请求过于频繁，请稍后再试");
                }
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            // Fail-open: do not block login when Redis is unavailable.
            log.warn("Login security checks skipped due to Redis error", e);
        }
    }

    public void onLoginSuccess(String username) {
        String safeUser = normalizeUser(username);
        if (!StringUtils.hasText(safeUser)) return;
        try {
            redis.delete(failUserKey(safeUser));
            redis.delete(lockUserKey(safeUser));
        } catch (Exception e) {
            log.warn("Failed to clear login failure counters", e);
        }
    }

    public void onLoginFailure(String username, String clientIp) {
        String safeIp = normalizeIp(clientIp);
        String safeUser = normalizeUser(username);
        try {
            if (StringUtils.hasText(safeUser)) {
                long userFails = incrementWithExpire(failUserKey(safeUser), userFailureWindow);
                if (userFails >= userMaxFailures) {
                    redis.opsForValue().set(lockUserKey(safeUser), "1", userLockDuration);
                }
            }

            long ipFails = incrementWithExpire(failIpKey(safeIp), ipFailureWindow);
            if (ipFails >= ipMaxFailures) {
                redis.opsForValue().set(lockIpKey(safeIp), "1", ipLockDuration);
            }
        } catch (Exception e) {
            log.warn("Login failure tracking skipped due to Redis error", e);
        }
    }

    private long incrementWithExpire(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        long n = value == null ? 0L : value;
        if (n == 1L) {
            redis.expire(key, ttl);
        } else {
            Long currentTtl = redis.getExpire(key, TimeUnit.SECONDS);
            if (currentTtl != null && currentTtl < 0) {
                redis.expire(key, ttl);
            }
        }
        return n;
    }

    private boolean isLocked(String lockKey) {
        if (!StringUtils.hasText(lockKey)) return false;
        return Boolean.TRUE.equals(redis.hasKey(lockKey));
    }

    private long getTtlSeconds(String key) {
        try {
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) return -1;
            return ttl;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private String lockMessage(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return "登录失败次数过多，请稍后再试";
        }
        return "登录失败次数过多，请稍后再试（约 " + ttlSeconds + "s）";
    }

    private String normalizeUser(String username) {
        if (!StringUtils.hasText(username)) return "";
        return username.trim();
    }

    private String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) return "unknown";
        String normalized = ip.trim();
        if (normalized.length() > 128) {
            normalized = normalized.substring(0, 128);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String rateIpKey(String ip) {
        return KEY_PREFIX + "rate:ip:" + ip;
    }

    private String rateUserKey(String user) {
        return KEY_PREFIX + "rate:user:" + user;
    }

    private String failIpKey(String ip) {
        return KEY_PREFIX + "fail:ip:" + ip;
    }

    private String failUserKey(String user) {
        return KEY_PREFIX + "fail:user:" + user;
    }

    private String lockIpKey(String ip) {
        return KEY_PREFIX + "lock:ip:" + ip;
    }

    private String lockUserKey(String user) {
        return KEY_PREFIX + "lock:user:" + user;
    }
}
