FROM eclipse-temurin:11

RUN mkdir -p /usr/src/app
# Create an empty metadata directory (it will be used as default by the file-based image manager)
RUN mkdir -p /usr/src/app/metadata
WORKDIR /usr/src/app

ADD target/platform-controller.jar target/platform-controller.jar

CMD ["java", "-cp", "target/platform-controller.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.controller.PlatformController"]
