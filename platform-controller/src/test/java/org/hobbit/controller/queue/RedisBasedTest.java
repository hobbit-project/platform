/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
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
