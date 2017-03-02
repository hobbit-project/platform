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
package org.hobbit.controller.data;

import java.io.Closeable;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.concurrent.Semaphore;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.controller.PlatformController;
import org.hobbit.controller.execute.ExperimentAbortTimerTask;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to store all the information that are needed to controle a
 * running experiment.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentStatus implements Closeable {

    /**
     * Typical states of a benchmark.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    public static enum States {
        /**
         * Benchmark and system are still initializing.
         */
        INIT("Benchmark and system are initializing."),
        /**
         * Benchmarking has started
         */
        STARTED("The benchmarking has started."),
        /**
         * The task generation has ended and the benchmark is evaluating.
         */
        EVALUATION("The task generation has ended and the benchmark is evaluating."),
        /**
         * The benchmark has stopped
         */
        STOPPED("The benchmark has stopped.");

        public final String description;

        private States(String description) {
            this.description = description;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentStatus.class);

    /**
     * Config of the current benchmark.
     */
    public final ExperimentConfiguration config;
    /**
     * URI of this experiment.
     */
    public final String experimentUri;
    /**
     * The timestamp in which this status object has been created.
     */
    private final long startTimeStamp;
    /**
     * State of the benchmark.
     */
    private States state = States.INIT;
    /**
     * Flag indicating whether the benchmark is ready.
     */
    private boolean benchmarkRunning = false;
    /**
     * Flag indicating whether the benchmark system is ready.
     */
    private boolean systemRunning = false;
    /**
     * Container name of the system.
     */
    private String benchmarkContainer = null;
    /**
     * Container name of the system.
     */
    private String systemContainer = null;
    /**
     * The RDF model containing the results.
     */
    private Model resultModel = null;
    /**
     * Mutex to make the access to the model thread safe.
     */
    private Semaphore modelMutex = new Semaphore(1);
    /**
     * Timer used to abort the experiment if it takes too much time.
     */
    private Timer abortTimer;

    public ExperimentStatus(ExperimentConfiguration config, String experimentUri) {
        this(config, experimentUri, null, 0, System.currentTimeMillis());
    }

    public ExperimentStatus(ExperimentConfiguration config, String experimentUri, long startTimeStamp) {
        this(config, experimentUri, null, 0, System.currentTimeMillis());
    }

    public ExperimentStatus(ExperimentConfiguration config, String experimentUri, PlatformController controller,
            long timeUntilAborting) {
        this(config, experimentUri, controller, timeUntilAborting, System.currentTimeMillis());
    }

    public ExperimentStatus(ExperimentConfiguration config, String experimentUri, PlatformController controller,
            long timeUntilAborting, long startTimeStamp) {
        this.config = config;
        this.experimentUri = experimentUri;
        this.startTimeStamp = startTimeStamp;

        if (controller != null) {
            abortTimer = new Timer();
            abortTimer.schedule(new ExperimentAbortTimerTask(controller, this), timeUntilAborting);
        }
    }

    public States getState() {
        return state;
    }

    public void setState(States state) {
        this.state = state;
    }

    public ExperimentConfiguration getConfig() {
        return config;
    }

    public boolean isBenchmarkRunning() {
        return benchmarkRunning;
    }

    public void setBenchmarkRunning(boolean benchmarkRunning) {
        this.benchmarkRunning = benchmarkRunning;
    }

    public boolean isSystemRunning() {
        return systemRunning;
    }

    public void setSystemRunning(boolean systemRunning) {
        this.systemRunning = systemRunning;
    }

    public String getBenchmarkContainer() {
        return benchmarkContainer;
    }

    public void setBenchmarkContainer(String benchmarkContainer) {
        this.benchmarkContainer = benchmarkContainer;
    }

    public String getSystemContainer() {
        return systemContainer;
    }

    public void setSystemContainer(String systemContainer) {
        this.systemContainer = systemContainer;
    }

    /**
     * The method sets a flag that (depending on the given flag) the system or
     * the benchmark is ready and returns <code>true</code> if internally both
     * have the state of being ready.
     * 
     * @param systemReportedReady
     *            <code>true</code> if the system is ready or <code>false</code>
     *            if the benchmark is ready
     * @return <code>true</code> if system and benchmark are ready
     */
    public synchronized boolean setReadyAndCheck(boolean systemReportedReady) {
        if (systemReportedReady) {
            systemRunning = true;
        } else {
            benchmarkRunning = true;
        }
        return systemRunning && benchmarkRunning;
    }

    public Model getResultModel() {
        return resultModel;
    }

    /**
     * Sets the result model if there is no model present. Otherwise, the given
     * model is merged with the existing model.
     * 
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param resultModel
     *            the new result model
     */
    public void setOrMergeResultModel(Model resultModel) {
        try {
            modelMutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for mutex of result model. Returning.");
            return;
        }
        try {
            if (this.resultModel == null) {
                this.resultModel = resultModel;
                addBasicInformation_Unsecured();
            } else {
                this.resultModel.add(resultModel);
            }
        } finally {
            modelMutex.release();
        }
    }

    /**
     * Sets the result model.
     * 
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param resultModel
     *            the new result model
     */
    public void setResultModel(Model resultModel) {
        try {
            modelMutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for mutex of result model. Returning.");
            return;
        }
        try {
            this.resultModel = resultModel;
            addBasicInformation_Unsecured();
        } finally {
            modelMutex.release();
        }
    }

    /**
     * Adds the given error to the result model if it does not already contain
     * an error.
     * 
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param error
     *            the error that should be added to the result model
     */
    public void addErrorIfNonPresent(Resource error) {
        try {
            modelMutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for mutex of result model. Returning.");
            return;
        }
        try {
            if (resultModel != null) {
                if (!resultModel.contains(resultModel.getResource(experimentUri), HOBBIT.terminatedWithError)) {
                    addError_Unsecured(error);
                }
            } else {
                initModel_Unsecured();
                addError_Unsecured(error);
            }
        } finally {
            modelMutex.release();
        }
    }

    /**
     * Adds the given error to the result model.
     * 
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param error
     *            the error that should be added to the result model
     */
    public void addError(Resource error) {
        try {
            modelMutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for mutex of result model. Returning.");
            return;
        }
        try {
            if (resultModel == null) {
                initModel_Unsecured();
            }
            addError_Unsecured(error);
        } finally {
            modelMutex.release();
        }
    }

    /**
     * Adds the given error to the result model.
     * 
     * <p>
     * This method is <b>not thread-safe</b>.
     * </p>
     * 
     * @param error
     *            the error that should be added to the result model
     */
    private void addError_Unsecured(Resource error) {
        if (this.resultModel == null) {
            return;
        }
        resultModel.add(resultModel.getResource(experimentUri), HOBBIT.terminatedWithError, error);
    }

    /**
     * Initializes the result model and adds basic information using
     * {@link #addBasicInformation_Unsecured()}.
     * 
     * <p>
     * This method is <b>not thread-safe</b>.
     * </p>
     */
    private void initModel_Unsecured() {
        resultModel = ModelFactory.createDefaultModel();
        addBasicInformation_Unsecured();
    }

    /**
     * Adds basic information to the result model.
     * 
     * <p>
     * This method is <b>not thread-safe</b>.
     * </p>
     */
    private void addBasicInformation_Unsecured() {
        if (this.resultModel == null) {
            return;
        }
        Resource experiment = resultModel.getResource(experimentUri);
        resultModel.add(experiment, RDF.type, HOBBIT.Experiment);
        resultModel.add(experiment, HOBBIT.hobbitPlatformVersion, PlatformController.PLATFORM_VERSION, "en");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(startTimeStamp);
        resultModel.add(experiment, HOBBIT.startTime, resultModel.createTypedLiteral(startDate));
        if (config.benchmarkUri != null) {
            resultModel.add(experiment, HOBBIT.involvesBenchmark, resultModel.getResource(config.benchmarkUri));
        }
        if (config.serializedBenchParams != null) {
            try {
                Model benchmarkParamModel = RabbitMQUtils.readModel(config.serializedBenchParams);
                StmtIterator iterator = benchmarkParamModel.listStatements(
                        benchmarkParamModel.getResource(Constants.NEW_EXPERIMENT_URI), null, (RDFNode) null);
                Statement statement;
                while (iterator.hasNext()) {
                    statement = iterator.next();
                    resultModel.add(experiment, statement.getPredicate(), statement.getObject());
                }
            } catch (Exception e) {
                LOGGER.info(
                        "Got an exception while trying to parse the serialized benchmark parameters. Won't be able to add them to the result model.",
                        e);
            }
        }
        if (config.systemUri != null) {
            resultModel.add(experiment, HOBBIT.involvesSystemInstance, resultModel.getResource(config.systemUri));
        }
        if (config.challengeTaskUri != null) {
            resultModel.add(experiment, HOBBIT.isPartOf, resultModel.getResource(config.challengeTaskUri));
        }
    }

    @Override
    public void close() throws IOException {
        if (abortTimer != null) {
            abortTimer.cancel();
        }
    }

}
