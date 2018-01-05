# HOBBIT platform

1. [Requirements](https://github.com/hobbit-project/platform#requirements)
1. [Preparing](https://github.com/hobbit-project/platform#preparing)
   1. [Configure Virtuoso](https://github.com/hobbit-project/platform#configure-virtuoso)
   1. [Configure Keycloak](https://github.com/hobbit-project/platform#configure-keycloak)
      1. [Details of the user management](https://github.com/hobbit-project/platform#details-of-the-user-management)
   1. [Using Benchmarks from the HOBBIT Git](https://github.com/hobbit-project/platform#using-benchmarks-from-the-hobbit-git)
1. [Running](https://github.com/hobbit-project/platform#running)
1. [Troubleshooting](https://github.com/hobbit-project/platform#troubleshooting)
1. [Related projects](https://github.com/hobbit-project/platform#related-projects)

## Requirements

* For building java projects
  * [Java](https://www.java.com) 1.8
  * [Apache Maven](http://maven.apache.org/) 3.0.5
* For building the GUI
  * [Node.js](https://nodejs.org) v6.9.1
  * [npm](https://www.npmjs.com/) 4.0.2
* For building and running project components
  * [Docker](https://www.docker.com/) 1.12.2
  * [Docker Compose](https://docs.docker.com/compose/) 1.8.1

(Newer versions should work as well)

## Preparing

These steps have to be done only once before starting the platform the first time.

1. Clone this repository
2. Create new hobbit networks:
    `docker network create hobbit-core` and
    `docker network create hobbit`
3. Build platform components by running:
    `make build`
4. Build and pull required docker containers by running:
    `docker-compose build`
5. Configure Virtuoso
   1. Change passwords (optional)
   1. Run initialization script (required)
6. Configure Keycloak
7. Add your personal Gitlab token

### Configure Virtuoso

#### Change passwords (optional)
The Virtuoso store has two users - the Virtuoso super user `dba` and a second user that is used by the platform called `HobbitPlatform`. By default, both have the word `Password` as password. If you want to use different passwords, you can change them during the initialization of the Virtuoso. Note, that while you can run the initialization script only once, you still can change the Virtuoso user credentials using the Virtuoso GUI, later on.

* Generate two passwords for the Virtuoso super user `dba` and a second user that is used by the platform called `HobbitPlatform`
* Open `config/db/storage-init.sh` and put the passwords into the following two lines
```bash
# Setup the HOBBIT Platform user
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.USER_CREATE ('HobbitPlatform', 'Password'); GRANT SPARQL_UPDATE TO "HobbitPlatform";"
...
# Finally, change the 'dba' password
/opt/virtuoso/bin/isql 1111 dba dba exec="user_set_password ('dba', 'Password');"
```

* Open `docker-compose.yml` in the platform directory and add the password for the `HobbitPlatform` user at
```yml
  # Storage service
  storage-service:
    ...
    environment:
      ...
      - SPARQL_ENDPOINT_USERNAME=HobbitPlatform
      - SPARQL_ENDPOINT_PASSWORD=Password
```

#### Run initialization script (required)

* Start the Virtuoso of the platform by running
```bash
docker-compose up virtuoso
```
* Execute the init script
```bash
run-storage-init.sh
```

### Configure Keycloak

To be able to use the graphical user interface the Keycloak user management is needed. Since a user has to communicate with the Keycloak instance, the GUI needs to know the *public* address of the Keycloak instance, i.e., the address that Keycloak has when connecting to it using a browser.
* If you are accessing it locally on the machine the Docker is running, find out the address of the Keycloak container

```docker inspect --format='{{.NetworkSettings.Networks.hobbit.IPAddress}}' platform2_keycloak_1```

* If you want to access the GUI from a different machine, use the host address of the computer keycloak container is running on. You then need to make sure the access to the docker container is allowed. Using Linux this is done as described below in the firewall adjustments.

In any case proceed as follows

* Determine this address and put it in the `docker-compose.yml` file into the `KEYCLOAK_AUTH_URL` line of the GUI:
```yml
  # HOBBIT GUI
  gui:
    ...
    environment:
      - KEYCLOAK_AUTH_URL=http://<myipaddress>:8181/auth
```

Give write access to the keycloak database by performing
```bash
 chmod 777 config/keycloak
 chmod 666 config/keycloak/*
```
in the platforms project directory.

If the address of the GUI will be *different* from `http://<myipaddress>:8080` (e.g., because of the reason explained above) you have to configure this address in Keycloak
* Start Keycloak by running
```bash
docker-compose up keycloak
```
* Open the address of Keycloak in the browser and click on `Administration Console`. Login using the username `admin` and the password `H16obbit`.
* Make sure that the realm `Hobbit` is selected in the left upper corner below the Keycloak logo
* Click on `Clients` in the left menu and click on the `Hobbit-GUI` client.
* Add the address of the GUI to the list `Valid Redirect URIs` (with a trailing star, e.g., `http://192.168.99.100:8080/*`) as well as to the list `Web Origins` and click on `save` at the bottom of the page

#### Firewall adjustments (Linux)

The serverbackend of the hobbitgui container needs access to keycloak via the external IP address. Therefore you may need to adapt the firewall rules. First find the IP range of the hobbit network:
```bash
docker network inspect hobbit | grep Gateway
```

Assuming you get something like "Gateway": "172.19.0.1" you have to find the matching network device:
```bash
ip addr | grep -B 2 172.19.0
```

If you get something like
```bash
6: br-5c9d73b080ad: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP
    link/ether 02:42:22:50:d4:8c brd ff:ff:ff:ff:ff:ff
    inet 172.19.0.1/16 scope global br-5c9d73b080ad
```
the network device name is `br-5c9d73b080ad`. If you have iptables as firewall use:
```bash
iptables -A INPUT -i br-5c9d73b080ad -j ACCEPT
```

If you have firewalld (fedora/centos7) use:
```bash
firewall-cmd --permanent --zone=trusted --change-interface=br-5c9d73b080ad
firewall-cmd --reload
```

#### Details of the user management

To manage users, groups and/or roles:
* login to the Keycloak Administration Console (e.g., http://localhost:8181/auth/admin user: admin, default password: 'H16obbit')
* select the realm `Hobbit`

For new users do not forget to check/assign the role mappings (tab 'Role Mappings'). The Hobbit-gui application uses following roles:
* `system-provider`, i.e., registered users
* `guest`
* `challenge-organiser` are registered users with the right to create challenges

The preconfigured Keycloak image has the following users (default password for all these users is `hobbit`):
* user `testuser` with the roles `system-provider`, `guest`
* user `system-provider` has role `system-provider`, `guest`
* user `guest` has role `guest`
* user `challenge-organiser` has role `challenge-organiser`

The admin used for the Keycloak configuration is not listed in the users of the `Hobbit` realm. You can change its password or add additional administrative users by choosing the `master` realm in the upper left corner.

### Using Benchmarks from the HOBBIT Git

For getting access to your Systems or Benchmarks that have been uploaded to the HOBBIT Git (http://git.project-hobbit.eu) the following steps are needed. Note that this is *the only way to introduce systems or benchmarks to the platform at the moment*.

Benchmarks should already be accessible if the project containing their meta data file is public. For accessing a public system, you need to define a user in your local Keycloak that has exactly the same user name as the user that owns the project in which the system meta data file is located. More information about user management can be found in the section [Details of the user management](https://github.com/hobbit-project/platform#details-of-the-user-management).

If a benchmark or system project is not public, you need to add your user name, your mail address and an access token of your user to the platforms compose file.
* Login to the Hobbit git, open the `profile settings`, click on `Access Token` and create a personal access token for using the API
* Open the docker-compose file and put the token in the platform-controller configuration
```yml
  platform-controller:
    ...
    environment:
      ...
      - GITLAB_USER=user
      - GITLAB_EMAIL=user@provider.org
      - GITLAB_TOKEN=ABC-XYZ
```

## Running

The local version uses plain text logging and connects the platform to your local docker host. There's also no restart policy defined.

1. Start the platform by running: `docker-compose up`

Available services
* http://localhost:8080/ Graphical user interface of the platform
* http://localhost:8081/ RabbitMQ GUI
* http://localhost:8181/ Keycloak GUI
* http://localhost:8890/ Virtuoso GUI (including a SPARQL endpoint)
* `5672` RabbitMQ communication port for adding additional components to the platform, e.g., for testing scenarios

### Running controller without docker

1. Start the platform components by running: `docker-compose -f docker-compose-local.yml up`
2. Start the platform-controller by running: `make local-controller`

## Troubleshooting

If you encounter problems setting up the platform, please have a look at our [FAQ](https://github.com/hobbit-project/platform/wiki/FAQ#platform-setup-questions).

### Regular cleaning of dangling Docker images
Regular clean up of docker images is required for HOBBIT platform. To perform clean up you need to install a cronjob as follows:
```
0 3 * * * docker rmi $(docker images -q -f "dangling=true") > /root/`date +\%Y\%m\%d\%H\%M\%S`-docker-rmi-dangling-cron.log
```

This will remove all dangling images at 3:00 A.M. (at night) every day.

To install crontab on several server you might want to use the following script:
```
#!/bin/bash

crontab -l > currentcron
echo "0 3 * * * docker rmi \$(docker images -q -f \"dangling=true\") > /root/\`date +\%Y\%m\%d\%H\%M\%S\`-docker-rmi-dangling-cron.log" >> currentcron
crontab currentcron
rm currentcron
```
The script can be shared across your cluster using e.g. NFS share and executed using following command:
```
for x in {1..7}; do ssh -K "myhobbitnodehostname$x" 'klsu -a /path/to/the/script.sh' ; done
```

# Related projects

There are some projects related to the platform

* [Core](https://github.com/hobbit-project/core) - Library containing core functionalities that ease the integration into the platform.
* [Evaluation storage](https://github.com/hobbit-project/evaluation-storage) - A default implementation of a benchmark component.
* [Platform](https://github.com/hobbit-project/platform) & The HOBBIT platform and a wiki containing tutorials.
* [Ontology](https://github.com/hobbit-project/ontology) & The HOBBIT ontology used to store data and described in D2.2.1 of the HOBBIT project.
