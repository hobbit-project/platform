FROM eclipse-temurin:11-focal

COPY target/analysis-component.jar .

CMD java -cp analysis-component.jar org.hobbit.core.run.ComponentStarter org.hobbit.analysis.AnalysisComponent
