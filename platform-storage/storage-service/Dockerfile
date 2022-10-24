FROM eclipse-temurin:11-focal

COPY target/storage-service.jar storage-service.jar

CMD ["java", "-cp", "storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]
