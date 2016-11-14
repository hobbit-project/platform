# Info
The HOBBIT GUI consists of two parts, the  `gui client` and a `gui server backend`. The gui is implemented with `Angular2`. The backend provides a REST interface communication with the `platform-controler` and the `storage-service`.

# Build image
## Install node.js
To build the `gui client` for the docker image at first `node.js` needs to be installed `https://nodejs.org/en/` (tested with v6.3 and v6.8).

## Build gui client
To build the `gui client` run the following commands
```
 > cd hobbit-gui/gui-client
 > npm install
 > npm run build
```	

## Build gui server backend
This step requires the previouse `Build gui client` step to be finished. The `maven package` requires the `gui client` to be build.
To build the war file of the HOBBIT gui run
```
> cd hobbit-gui/gui-serverbackend
> mvn package
```

## Generate the docker image
### Dockerfile
The docker image is created with the war file in the directory `hobbit/gui-serverbackend`
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

### Build docker image
Build the image with the following command line option. Be aware that the image name `gui` is reused in the `platform-controller` `docker-compose.yml`
```
> docker build -t gui .
```

# Add image to platform-controler
Add the following section to your `docker-compose.yml` of the platform-controler. If the name of the image is different, this need to be changed in the `docker-compose.yml` as well.
```yaml
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
      # this url must be accessible from both the gui container and the clients (web browser)
      - KEYCLOAK_AUTH_URL=http://192.168.56.105:8181/auth

```

# Allow gui access to external keycloak address
The serverbackend of the hobbitgui container needs access to keycloak via the external IP address.
Therefore you may need to adapt the firewall rules.
First find the IP range of the hobbit network:
```
docker network inspect hobbit | grep Gateway
```
Assuming you get something like "Gateway": "172.19.0.1" you have to find the matching network device:
```
ip addr | grep -B 2 172.19.0
```
If you get something like
```
6: br-5c9d73b080ad: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP
    link/ether 02:42:22:50:d4:8c brd ff:ff:ff:ff:ff:ff
    inet 172.19.0.1/16 scope global br-5c9d73b080ad
```
the network device name is `br-5c9d73b080ad`

If you have `iptables`as firewall use:
```
iptables -A INPUT -i br-5c9d73b080ad -j ACCEPT
```
If you have `firewalld` (fedora/centos7) use:
```
firewall-cmd --permanent --zone=trusted --change-interface=br-5c9d73b080ad
firewall-cmd --reload
```

# Keycloak adjustments
The external url of the Hobbit-GUI must be allowed in the Keycloak configuration.
Open the Keycloak Administration Console (e.g. http://192.168.56.105:8181/auth/admin),
go to the realm `Hobbit`, client `Hobbit-GUI`, tab `Settings`.
Add the Hobbit-GUI base URL both to `Valid Redirect URIs` and `Web Origins`.
