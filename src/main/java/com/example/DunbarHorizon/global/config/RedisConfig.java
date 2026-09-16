package com.example.DunbarHorizon.global.config;

import com.example.DunbarHorizon.social.application.dto.result.NodeGraphResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
// 캐시 어드바이스를 트랜잭션보다 바깥에 둔다. 두 어드바이저의 기본 order가 LOWEST_PRECEDENCE로
// 같아서, 명시하지 않으면 설정 클래스 등록 순서에 따라 순서가 뒤집힌다. 트랜잭션이 바깥으로
// 잡히면 캐시 히트에도 Neo4j 세션과 BEGIN·COMMIT 왕복이 붙는다.
@EnableCaching(order = 1000)
public class RedisConfig implements CachingConfigurer {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                );

        // NodeGraphResult는 record(final class)라 GenericJackson2JsonRedisSerializer의
        // NON_FINAL 타입 정책에서 제외되어 LinkedHashMap으로 역직렬화됨.
        // List<NodeGraphResult>는 Class<T>로 지정할 수 없어 JavaType 생성자를 사용한다.
        @SuppressWarnings("deprecation")
        Jackson2JsonRedisSerializer<List<NodeGraphResult>> networkGraphSerializer =
                new Jackson2JsonRedisSerializer<>(
                        TypeFactory.defaultInstance().constructCollectionType(List.class, NodeGraphResult.class)
                );
        RedisCacheConfiguration networkGraphConfig = defaultConfig
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(networkGraphSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("dunbar:network:default", networkGraphConfig)
                .withCacheConfiguration("dunbar:network:label", networkGraphConfig)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis 캐시 조회 실패 — cache miss로 처리 (cache={}, key={}): {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis 캐시 저장 실패 — 결과는 반환됨 (cache={}, key={}): {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis 캐시 evict 실패 (cache={}, key={}): {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis 캐시 clear 실패 (cache={}): {}", cache.getName(), e.getMessage());
            }
        };
    }
}
