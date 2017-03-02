FROM java

ADD target/analysis-component-1.0.0.jar /analysis/analysis-component.jar

WORKDIR /analysis

CMD java -cp analysis-component.jar org.hobbit.core.run.ComponentStarter org.hobbit.analysis.AnalysisComponent
