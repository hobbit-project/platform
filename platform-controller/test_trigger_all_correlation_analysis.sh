#!/bin/sh
cd $(dirname $0)
java -cp target/platform-controller.jar org.hobbit.core.run.ComponentStarter org.hobbit.controller.test.TriggerAllCorrelationAnalysis
