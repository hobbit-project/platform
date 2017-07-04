FROM maven

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ADD . /usr/src/app

#RUN mvn package -Dmaven.test.skip=true

CMD ["java", "-cp", "target/storage-service.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.storage.service.StorageService"]
