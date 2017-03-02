HOBBIT platform
---------

# Requirements

- Maven for building java projects
- Node.js and NPM for building GUI
- Docker and docker-compose for building and running all projects

# Preparing

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
6. Configure Keycloak
7. Add your personal Gitlab token

## Configure Virtuoso

* Write down two passwords for the Virtuoso super user `dba` and a second user that is used by the platform called `HobbitPlatform`
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
* Start the Virtuoso of the platform by running `docker-compose up virtuoso`
* Execute the `run_storage_init.sh` script

## Configure Keycloak

To be able to use the graphical user interface the Keycloak user management is needed.

* Pull the preconfigured keycloak image and start it 
```bash
docker push git.project-hobbit.eu:4567/gitadmin/hobbit-keycloak
docker run 
```

The Hobbit-gui application uses following roles:
* `system-provider`, i.e., registered users
* `guest`
* `challenge-organiser` are registered users with the right to create challenges

The preconfigured Keycloak image has following users (default password for all these users is `hobbit`):
* user `testuser` with the roles `system-provider`, `guest`
* user `system-provider` has role `system-provider`, `guest`
* user `guest` has role `guest`
* user `challenge-organiser` has role `challenge-organiser`

## Add Gitlab token

For getting access 

# Running

The local version uses plain text logging and connects the platform to your local docker host. There's also no restart policy defined.

1. Start the platform by running: `docker-compose up`

Available services
* :8080 Graphical user interface of the platform
* :8081 RabbitMQ GUI
* :8181 Keycloak GUI
* :8890 Virtuoso GUI (including a SPARQL endpoint)
* :5672 RabbitMQ communication port for adding additional components to the platform, e.g., for testing scenarios
