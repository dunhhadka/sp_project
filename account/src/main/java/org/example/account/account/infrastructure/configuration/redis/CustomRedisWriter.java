package org.example.account.account.infrastructure.configuration.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class CustomRedisWriter implements RedisCacheWriter {
    private static final long EXPIRE_TIME_IN_HOUR = 4;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Duration sleepTime;
    private final CacheStatisticsCollector statistics;

    public CustomRedisWriter(JedisConnectionFactory redisConnectionFactory) {
        this(redisConnectionFactory, Duration.ZERO, CacheStatisticsCollector.none());
    }

    public CustomRedisWriter(RedisConnectionFactory redisConnectionFactory, Duration sleepTime, CacheStatisticsCollector statistics) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.sleepTime = sleepTime;
        this.statistics = statistics;
    }

    @Override
    public byte[] get(String name, byte[] key) {
        Assert.notNull(name, "Name must be not null");
        Assert.notNull(key, "Key must not be null");

        log.info("get for {} {}", name, new String(key));

        byte[] result = execute(name, connection -> connection.get(key));

        statistics.incGets(name);

        if (result != null) {
            statistics.incHits(name);
        } else {
            statistics.incMisses(name);
        }

        return result;
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return null;
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(key, "Key must not be null");
        Assert.notNull(value, "Value must not be null");

        log.info("{} {} {}", name, new String(key), new String(value));

        execute(name, connection -> {
            var keySets = getKeySets(name, key);
            log.info("key = {}", keySets);

            if (keySets != null) {
                try {
                    connection.multi();
                    connection.sAdd(keySets.getBytes(), key);
                    connection.set(key, value, Expiration.from(EXPIRE_TIME_IN_HOUR, TimeUnit.HOURS), RedisStringCommands.SetOption.upsert());
                    connection.exec();
                } catch (Exception e) {
                    connection.discard();
                }
            } else if (shouldSetExpireWithin(ttl)) {
                connection.set(key, value, Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS), RedisStringCommands.SetOption.upsert());
            } else {
                connection.set(key, value);
            }
            return "OK";
        });

        statistics.incPuts(name);
    }

    private boolean shouldSetExpireWithin(Duration ttl) {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }

    private String getKeySets(String name, byte[] key) {
        String keyString = new String(key, StandardCharsets.UTF_8);
        if (!keyString.contains(".")) {
            return null;
        }
        int firstDot = keyString.indexOf(".");
        int secondDot = keyString.indexOf(".", firstDot + 1);
        keyString = keyString.substring(0, secondDot);
        return keyString;
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        return null;
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return new byte[0];
    }

    @Override
    public void remove(String name, byte[] key) {

    }

    @Override
    public void clean(String name, byte[] pattern) {

    }

    @Override
    public void clearStatistics(String name) {

    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        return null;
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return null;
    }

    private <T> T execute(String name, Function<RedisConnection, T> callback) {
        RedisConnection connection = redisConnectionFactory.getConnection();

        try {
            checkKeyUnlock(name, connection);
            return callback.apply(connection);
        } finally {
            connection.close();
        }
    }

    private void checkKeyUnlock(String name, RedisConnection connection) {
        if (!isLockingCacheWriter()) {
            return;
        }

        long lockWaitTimeNs = System.nanoTime();
        try {
            while (doCheckLock(name, connection)) {
                Thread.sleep(sleepTime.toMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            statistics.incLockTime(name, System.nanoTime() - lockWaitTimeNs);
        }
    }

    private boolean doCheckLock(String name, RedisConnection connection) {
        return Boolean.TRUE.equals(connection.exists(createCacheLogKey(name)));
    }

    private byte[] createCacheLogKey(String name) {
        return name.getBytes(StandardCharsets.UTF_8);
    }

    // dùng sleepTime làm lock => nếu sleepTime > 0 => key đang bị lock
    private boolean isLockingCacheWriter() {
        return !sleepTime.isZero() && !sleepTime.isNegative();
    }
}
