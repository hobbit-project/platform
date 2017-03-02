# HOBBIT GUI Server Backend

The HOBBIT GUI is used to select/configure/launch a benchmark and to visualize the results. 

## Endpoints
```
/                                 GET    Ping
```


## Building the docker image from source

1. Install note.js
		a. https://nodejs.org/en/ (tested with v6.3 and v6.8)
	
2. Build GUI client
	> cd hobbit/gui-client
	> npm install
	> npm run build
	
3. Build GUI backend
		a. This requires the previouse build of the client
	4. > cd hobbit/gui-serverbackend
	> mvn package
	
	5. Dockerfile - https://hub.docker.com/_/jetty/

```
        FROM jetty
        ADD ./target/gui-serverbackend.war $JETTY_BASE/webapps/ROOT.war
        
        RUN cd $JETTY_HOME
        RUN curl -O https://downloads.jboss.org/keycloak/2.3.0.Final/adapters/keycloak-oidc/keycloak-jetty93-adapter-dist-2.3.0.Final.zip
        RUN unzip keycloak-jetty93-adapter-dist-2.3.0.Final.zip
        RUN rm keycloak-jetty93-adapter-dist-2.3.0.Final.zip
        RUN cd $JETTY_BASE
        RUN java -jar $JETTY_HOME/start.jar --add-to-startd=keycloak
```
	6. Create image
> docker build -t gui .


## Running

- Development
```
mvn jetty:run
```


- Production
```
mvn package
```
Then deploy the war file from the target directory.
Alternatively use the jetty runner.
More details on jetty runner can be found here: https://wiki.eclipse.org/Jetty/Howto/Using_Jetty_Runner

## Configuration



## Platform Contoller - docker-compose.yml
Add the following section to your docker-compose.yml of the platform-controler

  hobbitgui:
    image: gui
    restart: always
    ports:
      - "8080:8080"
    networks:
      - hobbit-core
      - hobbit
    depends_on:
      - platform-controller
      - rabbit
      - virtuoso
    environment:
      - HOBBIT_RABBIT_HOST=rabbit



TODO