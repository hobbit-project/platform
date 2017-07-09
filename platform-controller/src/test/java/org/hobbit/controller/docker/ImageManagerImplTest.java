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
package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Created by Timofey Ermilov on 22/09/16.
 */
public class ImageManagerImplTest {
    private ImageManagerImpl imageManager;

    @Before
    public void initObserver() {
        imageManager = new ImageManagerImpl();
    }

    // @Test
    // public void modelToBenchmarkMetaDataTest() throws Exception {
    // Model m = imageManager.getBenchmarkModel("test");
    // BenchmarkMetaData data = imageManager.modelToBenchmarkMetaData(m);
    // assertEquals(data.benchmarkName, "GERBIL Benchmark");
    // assertEquals(data.benchmarkDescription, "Example of a HOBBIT T3.2
    // benchmark based on GERBIL");
    // assertEquals(data.benchmarkUri,
    // "http://example.org/GerbilBenchmark");
    // }

    @Test
    public void getBenchmarks() throws Exception {
        // use future to make test wait for async stuff (sigh, java)
        CompletableFuture<String> future = new CompletableFuture<>();

        // execute tests when gitlab is ready
        imageManager.runWhenGitlabIsReady(() -> {
            try {
            // System.out.println("Gitlab is ready! Running tests...");
            // get all benchmarks
            List<BenchmarkMetaData> bs = imageManager.getBenchmarks();
            Assert.assertTrue(bs.size() > 0);

            // get all systems
            List<SystemMetaData> sys = imageManager.getSystems();
            Assert.assertTrue(sys.size() > 0);

            // find gerbil benchmark
            BenchmarkMetaData gerbilBench = null;
            for (BenchmarkMetaData b : bs) {
                if (b.benchmarkUri.equals("http://w3id.org/gerbil/hobbit/vocab#GerbilBenchmarkA2KB")) {
                    gerbilBench = b;
                }
            }
            Assert.assertNotNull(gerbilBench);

            // find systems for gerbil
            List<SystemMetaData> gbSys = imageManager.getSystemsForBenchmark(gerbilBench.benchmarkUri);
            Assert.assertTrue(gbSys.size() > 1);

            // get gerbil benchmark by URL
            Model benchmarkModel = imageManager
                    .getBenchmarkModel("http://w3id.org/gerbil/hobbit/vocab#GerbilBenchmarkA2KB");
            Assert.assertNotNull(benchmarkModel);

            // make sure that the retrieved model contains only one single
            // system instance
            Model systemModel = imageManager.getSystemModel("http://gerbil.org/systems/AgdistisWS");
            Assert.assertNotNull(systemModel);
            Assert.assertEquals(1, RdfHelper.getSubjectResources(systemModel, RDF.type, HOBBIT.SystemInstance).size());

            // find test systems for gerbil by URL
            systemModel = imageManager.getSystemModel("http://example.org/DummySystemInstance1");
            Assert.assertNotNull(systemModel);
            Model otherSystemModel = imageManager.getSystemModel("http://example.org/DummySystemInstance2");
            Assert.assertNotNull(otherSystemModel);

            // check for systems of user
            List<SystemMetaData> systems = imageManager.getSystemsOfUser("DefaultHobbitUser");
            Assert.assertNotNull(systems);
            // make sure that the list is not empty
            systems = imageManager.getSystemsOfUser("testuser");
            Assert.assertTrue(systems.size() > 0);
            systems = imageManager.getSystemsOfUser("kleanthie.georgala");
            Assert.assertTrue(systems.size() > 0);

            List<SystemMetaData> systems4Benchmark = imageManager.getSystemsForBenchmark(benchmarkModel);
            String userName = "testuser";
            Set<SystemMetaData> userSystems = new HashSet<SystemMetaData>(imageManager.getSystemsOfUser(userName));
            List<SystemMetaData> filteredSystems = new ArrayList<>(systems4Benchmark.size());
            for (SystemMetaData s : systems4Benchmark) {
                if (userSystems.contains(s)) {
                    filteredSystems.add(s);
                }
            }
            systems4Benchmark = filteredSystems;
            Gson gson = new Gson();
            System.out.println(gson.toJson(filteredSystems));
            byte data[] = RabbitMQUtils.writeString(gson.toJson(filteredSystems));
            Collection<SystemMetaData> recSystems = gson.fromJson(RabbitMQUtils.readString(data),
                    new TypeToken<Collection<SystemMetaData>>() {
                    }.getType());
            System.out.println(recSystems.size());

            future.complete("done");
            } catch (Throwable t) {
                t.printStackTrace();
                future.complete("error");
            }
        });
        // System.out.println("Waiting for gitlab...");

        Assert.assertEquals("done", future.get());
    }
}
