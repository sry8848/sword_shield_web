package com.swordshield.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存支持工具类，提供缓存操作的便捷方法
 */
public final class CacheSupport {
    private static final Logger log = LoggerFactory.getLogger(CacheSupport.class);

    private CacheSupport() {}

    /**
     * 从缓存中获取值，如果缓存中不存在则通过loader加载
     *
     * @param cacheManager 缓存管理器，可以为null
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 加载器，用于在缓存未命中时加载数据
     * @param <T> 返回值类型
     * @return 包装在Optional中的缓存值或加载值
     */
    public static <T> Optional<T> getOptional(CacheManager cacheManager,
                                              String cacheName,
                                              Object key,
                                              Supplier<Optional<T>> loader) {
        if (loader == null) {
            return Optional.empty();
        }
        // 获取缓存实例，如果cacheManager或cacheName为null则返回null
        Cache cache = cacheManager == null ? null : cacheManager.getCache(cacheName);
        if (cache == null) {
            Optional<T> loaded = loader.get();
            return loaded == null ? Optional.empty() : loaded;
        }

        try {
            Optional<T> cached = cache.get(key, () -> {
                Optional<T> loaded = loader.get();
                return loaded == null ? Optional.empty() : loaded;
            });
            return cached == null ? Optional.empty() : cached;
        } catch (Cache.ValueRetrievalException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw e;
        } catch (RuntimeException e) {
            // Cache infra failed; fall back to loader (fail-open).
            log.warn("Cache get failed: cacheName={}, key={}", cacheName, key, e);
            Optional<T> loaded = loader.get();
            return loaded == null ? Optional.empty() : loaded;
        }
    }
}

