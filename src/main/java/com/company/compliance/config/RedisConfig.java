package com.company.compliance.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache and template configuration.
 *
 * <p>Cache regions and TTLs:
 * <ul>
 *   <li>{@code policies}       — 10 min   (read-heavy, invalidated on write)</li>
 *   <li>{@code users}          — 5  min   (shorter due to auth sensitivity)</li>
 *   <li>{@code organizations}  — 30 min   (rarely changes)</li>
 *   <li>{@code risk-scores}    — 15 min   (refreshed after each evaluation)</li>
 *   <li>{@code dashboard}      — 2  min   (fast stale tolerance for dashboards)</li>
 * </ul>
 *
 * <p>All cache keys are prefixed with {@code compliance:} (configured in application.yml).
 * Values are serialised as JSON (not Java serialisation) for interoperability
 * and forward compatibility.
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {

    // ── Jackson ObjectMapper for Redis ────────────────────────────

    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Embed type info so deserialisation knows the concrete class
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    // ── Generic Redis template ────────────────────────────────────

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setEnableTransactionSupport(false); // use separate Redis TX if needed
        template.afterPropertiesSet();
        return template;
    }

    // ── Cache manager ─────────────────────────────────────────────

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .prefixCacheNameWith("compliance:")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer));

        // Per-region TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("policies",
                defaults.entryTtl(Duration.ofMinutes(10)));

        cacheConfigs.put("users",
                defaults.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("organizations",
                defaults.entryTtl(Duration.ofMinutes(30)));

        cacheConfigs.put("risk-scores",
                defaults.entryTtl(Duration.ofMinutes(15)));

        cacheConfigs.put("dashboard",
                defaults.entryTtl(Duration.ofMinutes(2)));

        cacheConfigs.put("report-templates",
                defaults.entryTtl(Duration.ofHours(6)));   // very stable data

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
