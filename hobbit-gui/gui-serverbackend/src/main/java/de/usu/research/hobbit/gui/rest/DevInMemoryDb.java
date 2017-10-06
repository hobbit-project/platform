/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest;

import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkListBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengesListBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentCountBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentsListBean;
import de.usu.research.hobbit.gui.rest.beans.NamedEntityBean;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DevInMemoryDb {
    public static final DevInMemoryDb theInstance = new DevInMemoryDb();

    private List<ChallengeBean> challenges = new ArrayList<>();
    private static List<ExperimentBean> experiments = new ArrayList<>();
    private List<BenchmarkBean> benchmarks = new ArrayList<>();

    private AtomicInteger nextId = new AtomicInteger();

    public List<ChallengeBean> getChallenges() {
        loadChallengesIfNeeded();
        return new ArrayList<>(challenges);
    }

    private void loadChallengesIfNeeded() {
        try {
            synchronized (challenges) {
                if (challenges.isEmpty()) {
                    List<ChallengeBean> list = loadChallenges();
                    challenges.addAll(list);
                }
            }
        }
        catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void addChallenge(ChallengeBean challenge) {
        loadChallengesIfNeeded();
        challenge.setId("" + challenge.getName() + "_" + nextId.incrementAndGet());
        synchronized (challenges) {
            challenges.add(challenge);
        }
    }

    private List<ChallengeBean> loadChallenges() throws JAXBException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("/sample/challenges.json");

        JAXBElement<ChallengesListBean> elem = MarshallerUtil.unmarshall(input, ChallengesListBean.class);
        List<ChallengeBean> challenges = elem.getValue().getChallenges();
        return challenges;
    }

    public Boolean closeChallenge(String id) {
        synchronized (challenges) {
            for (int i = 0; i < challenges.size(); i++) {
                ChallengeBean item = challenges.get(i);
                if (item.getId().equals(id)) {
                    if (!item.isClosed()) {
                        item.setClosed(true);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
        }
        return null;
    }

    public String updateChallenge(ChallengeBean challenge) {
        synchronized (challenges) {
            for (int i = 0; i < challenges.size(); i++) {
                ChallengeBean item = challenges.get(i);
                if (item.getId().equals(challenge.getId())) {
                    challenges.set(i, challenge);
                    return challenge.getId();
                }
            }
        }
        return null;
    }

    public String deleteChallenge(String id) {
        synchronized (challenges) {
            for (int i = 0; i < challenges.size(); i++) {
                ChallengeBean item = challenges.get(i);
                if (item.getId().equals(id)) {
                    challenges.remove(i);
                    return id;
                }
            }
        }
        return null;
    }

    private void loadExperimentsIfNeeded() {
        try {
            synchronized (experiments) {
                if (experiments.isEmpty()) {
                    List<ExperimentBean> list = loadExperiments();
                    experiments.addAll(list);
                }
            }
        }
        catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ExperimentBean> loadExperiments() throws JAXBException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("/sample/experiments.json");

        JAXBElement<ExperimentsListBean> elem = MarshallerUtil.unmarshall(input, ExperimentsListBean.class);
        List<ExperimentBean> list = elem.getValue().getExperiments();
        return list;
    }

    private static class Counters {
        private Map<String, AtomicInteger> results = new HashMap<>();

        public void incr(String id) {
            AtomicInteger counter = results.get(id);
            if (counter == null) {
                results.put(id, new AtomicInteger(1));
            }
            else {
                counter.incrementAndGet();
            }
        }

        public int getCount(String id) {
            AtomicInteger counter = results.get(id);
            if (counter == null) {
                return 0;
            }
            else {
                return counter.get();
            }
        }
    }

    public List<ExperimentCountBean> countByChallengeTaskIds(String challengeId) {
        loadExperimentsIfNeeded();
        Counters counters = new Counters();
        List<ExperimentCountBean> results = new ArrayList<>();
        synchronized (experiments) {
            for (ExperimentBean bean : experiments) {
                counters.incr(bean.getChallengeTask().getId());
            }
            ChallengeBean challenge = getChallengeById(challengeId);
            if (challenge != null) {
                for (ChallengeTaskBean task : challenge.getTasks()) {
                    ExperimentCountBean countBean = new ExperimentCountBean();
                    countBean
                        .setChallengeTask(new NamedEntityBean(task.getId(), task.getName(), task.getDescription()));
                    countBean.setCount(counters.getCount(task.getId()));
                    results.add(countBean);
                }
            }
        }
        return results;
    }

    private ChallengeBean getChallengeById(String challengeId) {
        synchronized (challenges) {
            for (ChallengeBean challenge : challenges) {
                if (challenge.getId().equals(challengeId)) {
                    return challenge;
                }
            }
        }
        return null;
    }

    public List<ExperimentBean> queryExperiments(String[] ids, String challengeTaskId) {
        loadExperimentsIfNeeded();
        ArrayList<ExperimentBean> results = new ArrayList<>();
        synchronized (experiments) {
            if (ids != null) {
                for (String id : ids) {
                    for (ExperimentBean bean : experiments) {
                        if (bean.getId().equals(id)) {
                            results.add(bean);
                        }
                    }
                }
            }
            else if (challengeTaskId != null) {
                for (ExperimentBean bean : experiments) {
                    if (bean.getChallengeTask() != null && bean.getChallengeTask().getId().equals(challengeTaskId)) {
                        results.add(bean);
                    }
                }
            }
            else {
                results.addAll(experiments);
            }
        }
        return results;
    }

    private void loadBenchmarksIfNeeded() {
        try {
            synchronized (benchmarks) {
                if (benchmarks.isEmpty()) {
                    List<BenchmarkBean> list = loadBenchmarks();
                    benchmarks.addAll(list);
                }
            }
        }
        catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private List<BenchmarkBean> loadBenchmarks() throws JAXBException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("/sample/benchmark_DevDB.json");

        JAXBElement<BenchmarkListBean> elem = MarshallerUtil.unmarshall(input, BenchmarkListBean.class);
        List<BenchmarkBean> list = elem.getValue().getBenchmarks();
        return list;
    }

    public List<BenchmarkBean> getBenchmarks() {
        loadBenchmarksIfNeeded();
        return benchmarks;
    }
}
