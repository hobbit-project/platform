FROM eclipse-temurin:11

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ADD target/storage-service.jar target/storage-service.jar

CMD ["java", "-cp", "target/storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]
