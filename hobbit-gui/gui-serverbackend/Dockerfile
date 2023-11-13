# https://nodejs.org/en/docs/guides/nodejs-docker-webapp

# https://www.npmjs.com/package/node-sass#node-version-support-policy
# node-sass 4.10.0 (../gui-client/package-lock.json) ⇒ node <= 11

# Build web pages
FROM node:11 AS build-client
WORKDIR /usr/src/hobbit-platform
COPY hobbit-gui/gui-client/package*.json ./hobbit-gui/gui-client/
RUN npm --prefix hobbit-gui/gui-client ci --omit=dev
COPY hobbit-gui/gui-client ./hobbit-gui/gui-client
RUN npm --prefix hobbit-gui/gui-client run build-prod

# Build Java backend
FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /usr/src/hobbit-platform
COPY parent-pom/pom.xml ./parent-pom/
COPY hobbit-gui/gui-serverbackend/pom.xml ./hobbit-gui/gui-serverbackend/
RUN mvn --file ./hobbit-gui/gui-serverbackend/ dependency:go-offline
COPY hobbit-gui/gui-serverbackend/src ./hobbit-gui/gui-serverbackend/src
RUN mvn --file ./hobbit-gui/gui-serverbackend/ test
COPY --from=build-client /usr/src/hobbit-platform/hobbit-gui/gui-client/dist ./hobbit-gui/gui-client/dist
RUN mvn --file ./hobbit-gui/gui-serverbackend/ package

# Set up web server
FROM jetty:9.3-jre8
# Add script to register keycloak
COPY hobbit-gui/gui-serverbackend/configure-auth.sh $JETTY_BASE/configure-auth.sh
# Download the keycloak adapter and add it
RUN cd $JETTY_BASE && \
 curl -L -O http://hobbitdata.informatik.uni-leipzig.de/hobbit/keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip && \
 unzip keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 rm -f keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 java -jar $JETTY_HOME/start.jar --add-to-startd=keycloak
# Copy message definitions
COPY hobbit-gui/gui-serverbackend/messages /var/lib/jetty/webapps/messages
# Copy Java backend
COPY --chown=jetty --chmod=777 --from=build /usr/src/hobbit-platform/hobbit-gui/gui-serverbackend/target/gui-serverbackend $JETTY_BASE/webapps/ROOT

ENTRYPOINT ./configure-auth.sh && /docker-entrypoint.sh
