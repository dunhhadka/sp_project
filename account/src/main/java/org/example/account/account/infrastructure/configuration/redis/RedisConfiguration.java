package org.example.account.account.infrastructure.configuration.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisConfiguration {

    @Bean("redis-properties")
    @ConditionalOnMissingBean(name = "redis-properties")
    @ConfigurationProperties(prefix = "spring.redis-cache")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean("redis-connection")
    @ConditionalOnMissingBean(name = "redis-connection")
    public JedisConnectionFactory connectionFactory(@Qualifier("redis-properties") RedisProperties redisProperties) {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(redisProperties.getDatabase());

        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfigurationBuilder = JedisClientConfiguration.builder();
        jedisClientConfigurationBuilder.connectTimeout(Duration.ofMillis(redisProperties.getTimeout()));
        jedisClientConfigurationBuilder.readTimeout(Duration.ofMillis(redisProperties.getTimeout()));
        jedisClientConfigurationBuilder.usePooling().poolConfig(redisProperties.getPool());

        return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfigurationBuilder.build());
    }

    @Bean("redis-template")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("redis-connection") JedisConnectionFactory connectionFactory) {
        var redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(connectionFactory);
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(JedisConnectionFactory redisConnectionFactory) {
//        var redisCacheConfigurationBuilder = RedisCacheManager.RedisCacheManagerBuilder
//                .fromConnectionFactory(redisConnectionFactory)
//                .cacheDefaults(
//                        RedisCacheConfiguration.defaultCacheConfig()
//                                .disableCachingNullValues()
//                                .entryTtl(Duration.ofMinutes(2))
//                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
//                                        new GenericJackson2JsonRedisSerializer()
//                                ))
//                );
//        return redisCacheConfigurationBuilder.build();
        return RedisCacheManager
                .builder(new CustomRedisWriter(redisConnectionFactory))
                .build();
    }

}
