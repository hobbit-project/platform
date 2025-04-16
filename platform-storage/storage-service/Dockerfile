FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /usr/src/hobbit-platform
COPY parent-pom/pom.xml parent-pom/
RUN mvn --file parent-pom -Dmaven.test.skip=true install
WORKDIR /usr/src/hobbit-platform/platform-storage/storage-service
COPY platform-storage/storage-service/pom.xml .
RUN mvn dependency:go-offline
COPY platform-storage/storage-service/src src
RUN mvn -Dmaven.test.skip=true package
FROM eclipse-temurin:11
WORKDIR /app
COPY --from=build /usr/src/hobbit-platform/platform-storage/storage-service/target/storage-service.jar .
CMD ["java", "-cp", "storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]
