package org.hobbit.controller.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hobbit.controller.data.ExperimentConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Created by Timofey Ermilov on 07/09/16.
 */
public class ExperimentQueueImplTest extends RedisBasedTest {
    private ExperimentQueueImpl queue;

    private ExperimentConfiguration decodeExperimentFromString(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ExperimentConfiguration.class);
    }

    @Before
    public void init() {
        queue = new ExperimentQueueImpl();
    }

    @Test
    public void test() throws Exception {
        // create test config
        ExperimentConfiguration cfg = new ExperimentConfiguration();
        cfg.id = "1";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 5);
        Date d1 = cal.getTime();
        cfg.ExecutionDate = d1;
        // add to queue
        queue.add(cfg);

        // ask redis directly to make sure it's there
        // get from experiment data collection
        String str = redisSyncCommands.hget(ExperimentQueueImpl.EXPERIMENT_KEY, cfg.id);
        ExperimentConfiguration loadedCfg = decodeExperimentFromString(str);
        assertEquals(cfg.id, loadedCfg.id);
        // get from queue
        List<String> items = redisSyncCommands.zrangebyscore(ExperimentQueueImpl.EXPERIMENT_QUEUE, "-inf", "+inf", 0, 1);
        String ID = items.get(0);
        assertEquals(cfg.id, ID);

        // remove from queue
        queue.remove(cfg);
        // get from experiment data collection
        str = redisSyncCommands.hget(ExperimentQueueImpl.EXPERIMENT_KEY, cfg.id);
        assertNull(str);
        // get from queue
        items = redisSyncCommands.zrangebyscore(ExperimentQueueImpl.EXPERIMENT_QUEUE, "-inf", "+inf", 0, 1);
        assertEquals(items.size(), 0);

        // create test config 2
        ExperimentConfiguration cfg2 = new ExperimentConfiguration();
        cfg2.id = "2";
        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 4);
        Date d2 = cal.getTime();
        cfg2.ExecutionDate = d2;
        // add to queue
        queue.add(cfg);
        queue.add(cfg2);

        // get a list
        List<ExperimentConfiguration> all = queue.listAll();
        assertEquals(all.size(), 2);

        // create test config 3
        ExperimentConfiguration cfg3 = new ExperimentConfiguration();
        cfg3.id = "3";
        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 6);
        Date d3 = cal.getTime();
        cfg3.ExecutionDate = d3;
        queue.add(cfg3);
        // get next experiment
        ExperimentConfiguration next = queue.getNextExperiment();
        assertEquals(next.id, cfg3.id);
    }

    @Test
    public void addThenNextTest() {
        // create test config
        ExperimentConfiguration cfg = new ExperimentConfiguration();
        cfg.id = "1";
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 5);
        Date d1 = cal.getTime();
        cfg.ExecutionDate = d1;
        // add to queue
        queue.add(cfg);

        // get next experiment
        ExperimentConfiguration next = queue.getNextExperiment();
        assertEquals(next.id, cfg.id);

        // remove experiment
        queue.remove(cfg);

        // get next experiment
        next = queue.getNextExperiment();
        assertNull(next);
    }

    @After
    public void close() {
        // cleanup
        redisSyncCommands.del(ExperimentQueueImpl.EXPERIMENT_KEY, ExperimentQueueImpl.EXPERIMENT_QUEUE);
        // close
        queue.close();
    }
}
