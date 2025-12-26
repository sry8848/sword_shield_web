package com.swordshield.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache helper for "negative caching" (cache penetration protection):
 * cache {@code Optional.empty()} for a short TTL instead of repeatedly hitting DB for non-existent resources.
 * <p>
 * Fail-open: when cache infra is unavailable, it falls back to loader.
 */
public final class CacheSupport {
    private static final Logger log = LoggerFactory.getLogger(CacheSupport.class);

    private CacheSupport() {}

    public static <T> Optional<T> getOptional(CacheManager cacheManager,
                                              String cacheName,
                                              Object key,
                                              Supplier<Optional<T>> loader) {
        if (loader == null) {
            return Optional.empty();
        }
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

