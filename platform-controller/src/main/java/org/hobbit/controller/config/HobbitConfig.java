package org.hobbit.controller.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Tim Ermilov on 15.05.17.
 */
public class HobbitConfig {
    public class TimeoutConfig {
        public long benchmarkTimeout;
        public long challengeTimeout;

        public TimeoutConfig(long bt, long ct) {
            benchmarkTimeout = bt;
            challengeTimeout = ct;
        }
    }

    public Map<String, LinkedHashMap<String, Object>> timeouts;

    public static HobbitConfig loadConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String file = HobbitConfig.class.getClassLoader().getResource("config.yaml").getFile();
        HobbitConfig cfg = mapper.readValue(new File(file), HobbitConfig.class);
        return cfg;
    }

    public TimeoutConfig getTimeout(String benchmarkUrl) {
        LinkedHashMap item = timeouts.get(benchmarkUrl);
        if (item == null) {
            return null;
        }
        long bt = Integer.parseInt(item.get("benchmark").toString());
        long ct = Integer.parseInt(item.get("challenge").toString());
        return new TimeoutConfig(bt, ct);
    }


}
