package com.sivalabs.ft.features.config;

import com.sivalabs.ft.features.cache.SimpleMapCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new SimpleMapCacheManager();
    }
}
