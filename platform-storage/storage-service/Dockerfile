#FROM maven:3-eclipse-temurin-11 AS build
#WORKDIR /usr/src/hobbit-platform
#COPY parent-pom/pom.xml parent-pom/
#RUN mvn --file parent-pom -Dmaven.test.skip=true install
#ARG project=platform-storage/storage-service
#COPY ${project}/pom.xml ${project}/
#RUN mvn --file ${project} dependency:go-offline
#COPY ${project}/src ${project}/src
#RUN mvn --file ${project} -Dmaven.test.skip=true package
#
#FROM eclipse-temurin:11
#COPY --from=build /usr/src/hobbit-platform/platform-storage/storage-service/target/storage-service.jar .
#CMD ["java", "-cp", "storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]


# ---- Build Stage ----
FROM maven:3-eclipse-temurin-11 AS build

# Set the working directory
WORKDIR /usr/src/hobbit-platform

# Copy parent POM
COPY parent-pom/pom.xml parent-pom/
RUN mvn --file parent-pom -Dmaven.test.skip=true install

# Copy project files and build
WORKDIR /usr/src/hobbit-platform/platform-storage/storage-service
COPY platform-storage/storage-service/pom.xml .
RUN mvn dependency:go-offline

COPY platform-storage/storage-service/src src
RUN mvn -Dmaven.test.skip=true package

# ---- Runtime Stage ----
FROM eclipse-temurin:11

WORKDIR /app
COPY --from=build /usr/src/hobbit-platform/platform-storage/storage-service/target/storage-service.jar .

CMD ["java", "-cp", "storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]
