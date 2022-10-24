FROM eclipse-temurin:11-focal

# Create an empty metadata directory (it will be used as default by the file-based image manager)
RUN mkdir -p /usr/src/app/metadata

COPY target/platform-controller.jar platform-controller.jar

CMD ["java", "-cp", "platform-controller.jar", "org.hobbit.core.run.ComponentStarter", "org.hobbit.controller.PlatformController"]
