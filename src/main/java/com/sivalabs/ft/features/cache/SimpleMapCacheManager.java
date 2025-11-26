package com.sivalabs.ft.features.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

public class SimpleMapCacheManager implements CacheManager {
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public SimpleMapCacheManager() {
        // Pre-create the featuresByAssignee cache
        cacheMap.put("featuresByAssignee", new ConcurrentMapCache("featuresByAssignee"));
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, ConcurrentMapCache::new);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }
}
