FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /usr/src/hobbit-platform
COPY parent-pom/pom.xml ./parent-pom/
COPY hobbit-gui/gui-serverbackend/pom.xml ./hobbit-gui/gui-serverbackend/
RUN mvn --file ./hobbit-gui/gui-serverbackend/ dependency:go-offline
COPY hobbit-gui/gui-serverbackend/src ./hobbit-gui/gui-serverbackend/src
RUN mvn --file ./hobbit-gui/gui-serverbackend/ test
COPY hobbit-gui/gui-client/dist ./hobbit-gui/gui-client/dist
RUN mvn --file ./hobbit-gui/gui-serverbackend/ package

FROM jetty:9.3-jre8

RUN cd $JETTY_BASE && \
 curl -L -O http://hobbitdata.informatik.uni-leipzig.de/hobbit/keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip && \
 unzip keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 rm -f keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 java -jar $JETTY_HOME/start.jar --add-to-startd=keycloak

COPY hobbit-gui/gui-serverbackend/messages /var/lib/jetty/webapps/messages

COPY --from=build /usr/src/hobbit-platform/hobbit-gui/gui-serverbackend/target/gui-serverbackend.war $JETTY_BASE/webapps/ROOT.war
