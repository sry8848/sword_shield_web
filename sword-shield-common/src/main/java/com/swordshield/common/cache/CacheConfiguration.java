package com.swordshield.common.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
public class CacheConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);
    private static final String CACHE_PREFIX_VERSION = "v1";

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(
            ObjectMapper objectMapper,
            @Value("${spring.application.name:app}") String applicationName) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.swordshield.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper)))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "ss:cache:" + CACHE_PREFIX_VERSION + ":" + applicationName + ":" + cacheName + ":");
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration baseConfig,
            @Value("${app.cache.jitter-percent:0.1}") double jitterPercent,
            @Value("${app.cache.negative-ttl:30s}") Duration negativeTtl) {
        double safeJitterPercent = clamp(jitterPercent, 0.0, 0.5);
        RedisCacheConfiguration defaultConfig = baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(5), null, safeJitterPercent));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Content: list endpoints (short TTL)
        cacheConfigs.put("content:articles", baseConfig.entryTtl(ttlWithJitter(Duration.ofSeconds(60), null, safeJitterPercent)));
        cacheConfigs.put("content:exhibitors", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(5), null, safeJitterPercent)));
        cacheConfigs.put("content:achievements", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(5), null, safeJitterPercent)));
        cacheConfigs.put("content:materials", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(2), null, safeJitterPercent)));
        cacheConfigs.put("content:media", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(2), null, safeJitterPercent)));
        cacheConfigs.put("content:mediaByArticle", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(2), null, safeJitterPercent)));

        // Content: detail endpoints
        cacheConfigs.put("content:article", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(5), negativeTtl, safeJitterPercent)));
        cacheConfigs.put("content:exhibitor", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(10), negativeTtl, safeJitterPercent)));
        cacheConfigs.put("content:achievement", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(10), negativeTtl, safeJitterPercent)));
        cacheConfigs.put("content:material", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(10), negativeTtl, safeJitterPercent)));
        cacheConfigs.put("content:mediaItem", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(10), negativeTtl, safeJitterPercent)));

        // Activity
        cacheConfigs.put("activity:list", baseConfig.entryTtl(ttlWithJitter(Duration.ofSeconds(60), null, safeJitterPercent)));
        cacheConfigs.put("activity:detail", baseConfig.entryTtl(ttlWithJitter(Duration.ofMinutes(5), negativeTtl, safeJitterPercent)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private RedisCacheWriter.TtlFunction ttlWithJitter(Duration positiveTtl, Duration negativeTtl, double jitterPercent) {
        Duration positive = positiveTtl == null ? Duration.ZERO : positiveTtl;
        Duration negative = negativeTtl;
        double safeJitterPercent = clamp(jitterPercent, 0.0, 0.5);

        return (key, value) -> {
            Duration base = isNegativeValue(value) && negative != null ? negative : positive;
            if (base == null || base.isZero() || base.isNegative() || safeJitterPercent <= 0.0) {
                return base;
            }

            long baseMillis = base.toMillis();
            long jitterMillis = (long) (baseMillis * safeJitterPercent);
            if (jitterMillis <= 0) {
                return base;
            }
            long extraMillis = ThreadLocalRandom.current().nextLong(jitterMillis + 1);
            return base.plusMillis(extraMillis);
        };
    }

    private boolean isNegativeValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.isEmpty();
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache get failed: cacheName={}, key={}", safeName(cache), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache put failed: cacheName={}, key={}", safeName(cache), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict failed: cacheName={}, key={}", safeName(cache), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed: cacheName={}", safeName(cache), exception);
            }

            private String safeName(Cache cache) {
                try {
                    return cache == null ? "unknown" : cache.getName();
                } catch (Exception ignored) {
                    return "unknown";
                }
            }
        };
    }
}
