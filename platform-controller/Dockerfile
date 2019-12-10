FROM maven

RUN mkdir -p /usr/src/app
# Create an empty metadata directory (it will be used as default by the file-based image manager)
RUN mkdir -p /usr/src/app/metadata
WORKDIR /usr/src/app

ADD . /usr/src/app

#RUN mvn package -Dmaven.test.skip=true

CMD ["java", "-cp", "target/platform-controller.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.controller.PlatformController"]
