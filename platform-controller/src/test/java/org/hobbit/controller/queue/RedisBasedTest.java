package org.hobbit.controller.queue;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.After;
import org.junit.Before;

/**
 * Created by Timofey Ermilov on 07/09/16.
 */
public class RedisBasedTest {
    // redis connection
    protected RedisClient redisClient;
    protected StatefulRedisConnection<String, String> redisConnection;
    protected RedisCommands<String, String> redisSyncCommands;

    @Before
    public void initRedis() {
        // init redis redisConnection
        redisClient = RedisClient.create("redis://localhost");
        redisConnection = redisClient.connect();
        redisSyncCommands = redisConnection.sync();
    }

    @After
    public void closeRedis() {
        redisConnection.close();
        redisClient.shutdown();
    }
}
