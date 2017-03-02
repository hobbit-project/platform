# HOBBIT platform

1. [Requirements](https://github.com/hobbit-project/platform#requirements)
1. [Preparing](https://github.com/hobbit-project/platform#preparing)
   1. [Configure Virtuoso](https://github.com/hobbit-project/platform#configure-virtuoso)
   1. [Configure Keycloak](https://github.com/hobbit-project/platform#configure-keycloak)
   1. [Add Gitlab token](https://github.com/hobbit-project/platform#add-gitlab-token)
1. [Running](https://github.com/hobbit-project/platform#running)

## Requirements

- Maven for building java projects
- Node.js and NPM for building GUI
- Docker and docker-compose for building and running all projects

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
6. Configure Keycloak
7. Add your personal Gitlab token

### Configure Virtuoso

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
* Start the Virtuoso of the platform by running 
```bash
docker-compose up virtuoso
```
* Execute the `run_storage_init.sh` script

### Configure Keycloak

To be able to use the graphical user interface the Keycloak user management is needed. Since a user has to communicate with the Keycloak instance, the GUI needs to know the *public* address of the Keycloak instance, i.e., the address that Keycloak has when connecting to it using a browser. Unfortunately, even in a local setup this address can differ depending on the Docker installation you might have. For Linux users, the address is in most cases `localhost:8181` while for MS Windows users it depends on the VM that might be used to execute the Docker engine, e.g., `192.168.99.100:8181`.

* Determine this address and put it into the `KEYCLOAK_AUTH_URL` line of the GUI:
```yml
  # HOBBIT GUI
  gui:
    ...
    environment:
      - KEYCLOAK_AUTH_URL=http://192.168.99.100:8181/auth
```

If the address of the GUI will be *different* from `http:localhost:8080` (e.g., because of the reason explained above) you have to configure this address in Keycloak
* Start Keycloak by running
```bash
docker-compose up keycloak
```
* Open the address of Keycloak in the browser and click on `Administration Console`. Login using the username `admin` and the password `H16obbit`.
* Make sure that the realm `Hobbit` is selected in the left upper corner below the Keycloak logo
* Click on `Clients` in the left menu and click on the `Hobbit-GUI` client.
* Add the address of the GUI to the list `Valid Redirect URIs` (with a trailing star, e.g., `http://192.168.99.100:8080/*`) as well as to the list `Web Origins` and click on `save` at the bottom of the page

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

If a benchmark or system project is not public, you need to add an access token of your user to the platforms compose file.
* Login to the Hobbit git, open the `profile settings`, click on `Access Token` and create a personal access token for using the API
* Open the docker-compose file and put the token in the platform-controller configuration
```yml
  platform-controller:
    ...
    environment:
      ...
      - GITLAB_TOKEN=ABC-XYZ
```

## Running

The local version uses plain text logging and connects the platform to your local docker host. There's also no restart policy defined.

1. Start the platform by running: `docker-compose up`

Available services
* :8080 Graphical user interface of the platform
* :8081 RabbitMQ GUI
* :8181 Keycloak GUI
* :8890 Virtuoso GUI (including a SPARQL endpoint)
* :5672 RabbitMQ communication port for adding additional components to the platform, e.g., for testing scenarios
