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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration baseConfig) {
        RedisCacheConfiguration defaultConfig = baseConfig.entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Content: list endpoints (short TTL)
        cacheConfigs.put("content:articles", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("content:exhibitors", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("content:achievements", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("content:materials", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigs.put("content:media", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigs.put("content:mediaByArticle", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Content: detail endpoints
        cacheConfigs.put("content:article", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("content:exhibitor", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("content:achievement", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("content:material", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("content:mediaItem", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Activity
        cacheConfigs.put("activity:list", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("activity:detail", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
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
