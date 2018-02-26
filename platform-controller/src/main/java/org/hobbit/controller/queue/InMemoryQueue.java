package org.hobbit.controller.queue;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.hobbit.controller.data.ExperimentConfiguration;

/**
 * A simple in-memory implementation of the {@link ExperimentQueue} interface
 * that can be used for JUnit tests.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class InMemoryQueue implements ExperimentQueue {

    private Deque<ExperimentConfiguration> queue = new LinkedList<>();

    @Override
    public ExperimentConfiguration getNextExperiment() {
        synchronized (queue) {
            return queue.peek();
        }
    }

    @Override
    public void add(ExperimentConfiguration experiment) {
        synchronized (queue) {
            queue.add(experiment);
        }
    }

    @Override
    public boolean remove(ExperimentConfiguration experiment) {
        synchronized (queue) {
            return queue.remove(experiment);
        }
    }

    @Override
    public List<ExperimentConfiguration> listAll() {
        synchronized (queue) {
            return new ArrayList<>(queue);
        }
    }

    @Override
    public ExperimentConfiguration getExperiment(String experimentId) {
        return queue.stream().filter(e -> e.id.equals(experimentId)).findFirst().orElse(null);
    }

}
