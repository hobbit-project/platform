package org.hobbit;

import com.google.gson.JsonObject;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.queue.ExperimentQueueImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
public class QueueClient {
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    String id;
    ExperimentQueueImpl queue;

    @Before
    public void init(){

        //environmentVariables.set("HOBBIT_REDIS_HOST", "10.67.43.25:6379");

        queue = new ExperimentQueueImpl();
        id = "http://w3id.org/hobbit/experiments#"+String.valueOf(String.valueOf(new Date().getTime()));

    }

    @Test
    @Ignore
    public void flushQueue(){
        int deleted=0;
        for(ExperimentConfiguration configuration :  queue.listAll()){
            queue.remove(configuration);
            deleted++;
        }
        System.out.println(String.valueOf(deleted)+" experiments deleted");
    }

    @Test
    @Ignore
    public void submitToQueue(){
        //submitToQueue(BENCHMARK_URI, SYSTEM_URI);
    }

    private void submitToQueue(String benchmarkUri, String systemUri){

        ExperimentConfiguration cfg = new ExperimentConfiguration();
        cfg.id = id;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 5);
        cfg.executionDate = cal;
        cfg.benchmarkUri = benchmarkUri;
        cfg.systemUri = systemUri;
        cfg.userName = "";

        cfg.serializedBenchParams = "";

        queue.add(cfg);

        System.out.println("Experiment submitted");
    }
}
