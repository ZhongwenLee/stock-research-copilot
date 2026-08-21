package com.stockresearch.copilot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Hot-data caching. Prefers Redis when available; falls back to in-memory for tests/local.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String COMPANIES = "companies";
	public static final String READY_DOCS = "readyDocs";

	@Bean
	@ConditionalOnBean(RedisConnectionFactory.class)
	public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.disableCachingNullValues()
				.serializeKeysWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new GenericJackson2JsonRedisSerializer()));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaults)
				.withCacheConfiguration(COMPANIES, defaults.entryTtl(Duration.ofMinutes(30)))
				.withCacheConfiguration(READY_DOCS, defaults.entryTtl(Duration.ofMinutes(5)))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(CacheManager.class)
	public CacheManager simpleCacheManager() {
		return new ConcurrentMapCacheManager(COMPANIES, READY_DOCS);
	}
}
