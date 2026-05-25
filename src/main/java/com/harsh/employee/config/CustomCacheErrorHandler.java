package com.harsh.employee.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.error("Redis unreachable on GET for cache {}. Falling back to DB: {}", cache.getName(), exception.getMessage());
    }
    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.error("Redis unreachable on PUT for cache {}: {}", cache.getName(), exception.getMessage());
    }
    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.error("Redis unreachable on EVICT for cache {}: {}", cache.getName(), exception.getMessage());
    }
    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error("Redis unreachable on CLEAR for cache {}: {}", cache.getName(), exception.getMessage());
    }
}
