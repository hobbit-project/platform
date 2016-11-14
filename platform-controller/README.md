HOBBIT Platform Controller
===========

## Deploying to docker

1. Checkout this project.
1. Create new hobbit networks: 
`docker network create hobbit-core` and 
`docker network create hobbit`
2. Build platform controller with `mvn clean package -U -Dmaven.test.skip=true` and `docker-compose build`
3. Checkout the [platform-storage project](https://git.informatik.uni-leipzig.de/hobbit/platform-storage)
4. Create the storage image as described [here](https://git.informatik.uni-leipzig.de/hobbit/platform-storage/tree/master/virtuoso-docker) and copy the `virtuoso.ini` file into the `platform-controller/db` directory.
5. Build the storage-service by running `mvn clean package -U` and `docker build -t storage-service .` in `platform-storage/storage-service`
3. Start the platform controller by running `docker-compose up` in the platform-controller project directory.

After that Kibana is available on Port 5601.

## Starting benchmark

At the moment only one benchmark is hardcoded in the platform. However, starting it will cause an error since the images have to be build.
1. This can be done by checking out [WP3](https://git.informatik.uni-leipzig.de/hobbit/workpackage3/tree/master/T3.2/implementation) and calling `make` in the `workpackage3/T3.2/implementation` directory.
2. The front end mockup can be used by starting `test_cmd.sh` in the platform-controller project. It will create a new docker container including the project directory.
3. Inside the created container, navigate to `/usr/src/app` and execute `test_start_benchmark.sh`