FROM jetty:9.3-jre8

RUN cd $JETTY_BASE && \
 curl -L -O http://hobbitdata.informatik.uni-leipzig.de/hobbit/keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip && \
 unzip keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 rm -f keycloak-jetty93-adapter-for-hobbit-dist-2.4.0.Final.zip  && \
 java -jar $JETTY_HOME/start.jar --add-to-startd=keycloak

ADD ./messages /var/lib/jetty/webapps/messages

ADD ./target/gui-serverbackend.war $JETTY_BASE/webapps/ROOT.war
