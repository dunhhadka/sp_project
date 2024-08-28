package org.example.order.order.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisOrderCacheConfig {

    @Bean("redis-connection-order-cache")
    public JedisConnectionFactory redisConnectionFactory(@Qualifier("redis-order-cache-properties") RedisProperties redisProperties) {

        log.info("redis-connection-order-cache" + 1);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(redisProperties.getDatabase());

        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfiguration = JedisClientConfiguration.builder();
        jedisClientConfiguration.connectTimeout(Duration.ofMillis(redisProperties.getTimeout()));
        jedisClientConfiguration.readTimeout(Duration.ofMillis(redisProperties.getTimeout()));

        return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration.build());
    }

    @Bean("redis-order-cache-properties")
    @ConditionalOnMissingBean(name = "redis-order-cache-properties")
    @ConfigurationProperties(prefix = "spring.redis-order-cache")
    public RedisProperties redisOrderCacheProperties() {
        log.info("redis-order-cache-properties" + 2);
        return new RedisProperties();
    }

    @Primary
    @Bean("redis-template-order-cache")
    public RedisTemplate<String, String> redisTemplate(@Qualifier("redis-connection-order-cache") RedisConnectionFactory connectionFactory) {
        log.info("redis-template-order-cache" + 3);
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
