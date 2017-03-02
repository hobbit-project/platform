FROM jetty:9.3-jre8

RUN cd $JETTY_HOME && \
 curl -O https://downloads.jboss.org/keycloak/2.3.0.Final/adapters/keycloak-oidc/keycloak-jetty93-adapter-dist-2.3.0.Final.zip && \
 unzip keycloak-jetty93-adapter-dist-2.3.0.Final.zip && \
 rm -f keycloak-jetty93-adapter-dist-2.3.0.Final.zip && \
 cd $JETTY_BASE && \
 java -jar $JETTY_HOME/start.jar --add-to-startd=keycloak

ADD ./target/gui-serverbackend.war $JETTY_BASE/webapps/ROOT.war
