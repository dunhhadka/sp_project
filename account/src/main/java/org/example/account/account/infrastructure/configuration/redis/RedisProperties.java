package org.example.account.account.infrastructure.configuration.redis;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisPoolConfig;

@Getter
@Setter
public class RedisProperties {
    private int port;
    private String host;
    private String password = null;
    private int database;
    private int timeout;
    private JedisPoolConfig pool;
}
