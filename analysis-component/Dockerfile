FROM java

ADD target/analysis-component.jar /analysis/analysis-component.jar

WORKDIR /analysis

CMD java -cp analysis-component.jar org.hobbit.core.run.ComponentStarter org.hobbit.analysis.AnalysisComponent
