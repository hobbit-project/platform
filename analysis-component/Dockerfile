FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /usr/src/hobbit-platform
COPY parent-pom/pom.xml parent-pom/
ARG project=analysis-component
COPY ${project}/pom.xml ${project}/
RUN mvn --file ${project} dependency:go-offline
COPY ${project}/src ${project}/src
RUN mvn --file ${project} -Dmaven.test.skip=true package

FROM eclipse-temurin:11
COPY --from=build /usr/src/hobbit-platform/analysis-component/target/analysis-component.jar .
CMD ["java", "-cp", "analysis-component.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.analysis.AnalysisComponent"]
