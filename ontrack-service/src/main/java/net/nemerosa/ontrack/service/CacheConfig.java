package net.nemerosa.ontrack.service;

import net.nemerosa.ontrack.common.Caches;
import net.nemerosa.ontrack.service.support.GuavaCacheFactoryBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
// FIXME Uses a cache which can be configured using an external file
public class CacheConfig {

    private static final int ONE_DAY = 24 * 60;

    @Bean
    public CacheManager cacheManager() throws Exception {
        SimpleCacheManager o = new SimpleCacheManager();
        o.setCaches(
                Arrays.asList(
                        new GuavaCacheFactoryBean(Caches.SETTINGS, 1, ONE_DAY).getObject(),
                        new GuavaCacheFactoryBean(Caches.PROJECTS, 50, ONE_DAY).getObject()
                )
        );
        return o;
    }
}
