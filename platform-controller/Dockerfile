FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /usr/src/hobbit-platform
COPY parent-pom/pom.xml parent-pom/
ARG project=platform-controller
COPY ${project}/pom.xml ${project}/
RUN mvn --file ${project} dependency:go-offline
COPY ${project}/src ${project}/src
RUN mvn --file ${project} -Dmaven.test.skip=true package

FROM eclipse-temurin:11

# Create an empty metadata directory (it will be used as default by the file-based image manager)
RUN mkdir -p /usr/src/app/metadata

COPY --from=build /usr/src/hobbit-platform/platform-controller/target/platform-controller.jar .

CMD ["java", "-cp", "platform-controller.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.controller.PlatformController"]
