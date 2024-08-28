package org.example.order.order.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisPoolConfig;

@Getter
@Setter
public class RedisProperties {
    private String host;
    private int port = 6379;
    private String password = null;
    private int database = 0;
    private int timeout = 2000;
    private JedisPoolConfig pool;
}
