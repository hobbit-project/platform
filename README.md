HOBBIT platform
---------

# Requirements

- Maven for building java projects
- Node.js and NPM for building GUI
- Docker and docker-compose for building and running all projects

# Preparing

1. Clone this repository
2. Create new hobbit networks: `docker network create hobbit-core` and `docker network create hobbit`
3. Build platform components by running: `make build`
4. Build and pull required docker containers by running: `docker-compose build`

# Running localy

Local version uses plain text logging and connects platform to your local docker host.
There's also no restart policy defined.

1. Start the platform by running: `docker-compose up`

# Running deployment version

Deployment version uses ELK for logging and connects platform to internal docker host.
There's also autorestart policy defined.

1. Start platform by running: `docker-compose -f docker-compose-deploy.yml up`

# Create Hobbit user

Unfortunately, a user for the triple store has to be created manually, at the moment.

1. Open the Virtuoso GUI that is available on port 8890
2. Login with username `dba` and password `dba`
3. Go to `System Admin` > `User Accounts` and click on `Create New Account` in the right upper corner of the table
4. Create a user with the name `HobbitPlatform`, password `HobbitPlatformStorage` and the role `SPARQL_UPDATE`
