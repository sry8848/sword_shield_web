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

/**
 * 缓存配置类，用于配置Redis缓存管理器和相关缓存策略
 * 包含缓存序列化配置、TTL配置、错误处理等功能
 */
@Configuration
@EnableCaching
public class CacheConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);
    private static final String CACHE_PREFIX_VERSION = "v1";

    /**
     * 配置Redis缓存序列化配置
     * 设置对象序列化方式、缓存前缀等基础配置
     *
     * @param objectMapper 用于JSON序列化的ObjectMapper实例
     * @param applicationName 应用程序名称，用于构建缓存键前缀
     * @return Redis缓存配置对象
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(
            ObjectMapper objectMapper,
            @Value("${spring.application.name:app}") String applicationName) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        // 配置多态类型验证，允许特定包下的类型序列化
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

    /**
     * 创建缓存管理器，配置不同缓存的TTL策略
     * 包括内容缓存、活动缓存等不同业务场景的缓存配置
     *
     * @param connectionFactory Redis连接工厂
     * @param baseConfig 基础缓存配置
     * @param jitterPercent TTL抖动百分比，用于避免缓存雪崩
     * @param negativeTtl 负值缓存的TTL时间
     * @return Redis缓存管理器
     */
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

    /**
     * 计算带抖动的TTL函数
     * 为缓存过期时间添加随机抖动，避免缓存雪崩问题
     *
     * @param positiveTtl 正常值的TTL时间
     * @param negativeTtl 负值（如空值）的TTL时间
     * @param jitterPercent 抖动百分比
     * @return TTL计算函数
     */
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

    /**
     * 判断值是否为负值（如Optional.empty()）
     * 用于区分正常值和空值，以便应用不同的TTL策略
     *
     * @param value 待判断的值
     * @return 如果是负值返回true，否则返回false
     */
    private boolean isNegativeValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.isEmpty();
        }
        return false;
    }

    /**
     * 将数值限制在指定范围内
     * 确保抖动百分比在合理范围内，防止异常值影响缓存策略
     *
     * @param value 待限制的数值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的数值
     */
    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 创建缓存错误处理器
     * 处理缓存操作中的异常情况，记录警告日志但不中断业务流程
     *
     * @return 缓存错误处理器
     */
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
