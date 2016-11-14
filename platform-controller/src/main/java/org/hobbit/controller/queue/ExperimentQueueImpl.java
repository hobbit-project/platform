package org.hobbit.controller.queue;

import java.io.Closeable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hobbit.controller.data.ExperimentConfiguration;

import com.google.gson.Gson;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * Created by Timofey Ermilov on 07/09/16. TODO: 1. Setup Redis-based queue for
 * normal experiments 2. Setup Redis-based queue for challenge experiments 3.
 * Way to get next task in queue, prioritise challenges if challenge.startTime =
 * now
 */
public class ExperimentQueueImpl implements ExperimentQueue, Closeable {
    public final static String CHALLENGE_KEY = "challenge";
    public final static String EXPERIMENT_KEY = "experiment";
    public final static String CHALLENGE_QUEUE = "challenge_queue";
    public final static String EXPERIMENT_QUEUE = "experiment_queue";

    // redis connection
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    private RedisCommands<String, String> redisSyncCommands;

    public ExperimentQueueImpl() {
        // init redis redisConnection
        String host = "redis://localhost";
        if (System.getenv().containsKey("HOBBIT_REDIS_HOST")) {
            host = "redis://" + System.getenv().get("HOBBIT_REDIS_HOST");
        }
        redisClient = RedisClient.create(host);
        redisConnection = redisClient.connect();
        redisSyncCommands = redisConnection.sync();
    }

    private ExperimentConfiguration decodeExperimentFromString(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ExperimentConfiguration.class);
    }

    @Override
    public ExperimentConfiguration getNextExperiment() {
        String timestamp = Long.toString(new Timestamp(System.currentTimeMillis()).getTime());
        List<String> experimentIds = redisSyncCommands.zrangebyscore(EXPERIMENT_QUEUE, "-inf", timestamp);
        List<String> challengeIds = redisSyncCommands.zrangebyscore(CHALLENGE_QUEUE, "-inf", timestamp);
        if (experimentIds.isEmpty() && challengeIds.isEmpty()) {
            return null;
        }

        ExperimentConfiguration experiment = null;
        ExperimentConfiguration challenge = null;

        if (!experimentIds.isEmpty()) {
            String experimentId = experimentIds.get(experimentIds.size() - 1);
            String experimentStr = redisSyncCommands.hget(EXPERIMENT_KEY, experimentId);
            experiment = decodeExperimentFromString(experimentStr);
        }

        if (!challengeIds.isEmpty()) {
            String challengeId = challengeIds.get(challengeIds.size() - 1);
            String challengeStr = redisSyncCommands.hget(CHALLENGE_KEY, challengeId);
            challenge = decodeExperimentFromString(challengeStr);
        }

        if (challenge != null && experiment != null && challenge.ExecutionDate.before(experiment.ExecutionDate)) {
            return challenge;
        }

        if (challenge != null && experiment == null) {
            return challenge;
        }

        return experiment;
    }

    @Override
    public void add(ExperimentConfiguration experiment) {
        Gson gson = new Gson();
        String typeKey = EXPERIMENT_KEY; // TODO: detect type based on
                                         // experiment
        String queueKey = EXPERIMENT_QUEUE; // TODO: detect type based on
                                            // experiment
        String idKey = experiment.id; // TODO: correctly define experiment ID
        String experimentJson = gson.toJson(experiment);

        // add to experiment data store
        redisSyncCommands.hset(typeKey, idKey, experimentJson);
        long timestamp = 0;
        if (experiment.ExecutionDate != null) {
            timestamp = experiment.ExecutionDate.getTime();
        }
        // append it to queue
        redisSyncCommands.zadd(queueKey, (double) timestamp, idKey);
    }

    @Override
    public void remove(ExperimentConfiguration experiment) {
        String typeKey = EXPERIMENT_KEY; // TODO: detect type based on
                                         // experiment
        String queueKey = EXPERIMENT_QUEUE; // TODO: detect type based on
                                            // experiment
        String idKey = experiment.id; // TODO: correctly define experiment ID
        // remove from experiment data store
        redisSyncCommands.hdel(typeKey, idKey);
        // remove from queue
        redisSyncCommands.zrem(queueKey, idKey);
    }

    private List<ExperimentConfiguration> stringMapToExperimentList(Map<String, String> entries) {
        List<ExperimentConfiguration> result = new ArrayList<>();
        // decode and append all entries to result
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            ExperimentConfiguration c = decodeExperimentFromString(entry.getValue());
            result.add(c);
        }
        return result;
    }

    @Override
    public List<ExperimentConfiguration> listAll() {
        // get all experiments and challenges
        Map<String, String> experiments = redisSyncCommands.hgetall(EXPERIMENT_KEY);
        Map<String, String> challenges = redisSyncCommands.hgetall(CHALLENGE_KEY);
        // create result
        List<ExperimentConfiguration> result = stringMapToExperimentList(experiments);
        result.addAll(stringMapToExperimentList(challenges));
        // return result
        return result;
    }

    public void close() {
        redisConnection.close();
        redisClient.shutdown();
    }
}
