# build platform components
default: build

redeploy: build start

redeploy-gui:
	cd hobbit-gui/gui-client && npm install && npm run build-prod
	cd hobbit-gui/gui-serverbackend && mvn clean package
	docker-compose build
	docker stack deploy --compose-file docker-compose-dev.yml platform


redeploy-storage:
	cd platform-storage/storage-service && mvn clean package -U
	docker-compose build
	docker stack deploy --compose-file docker-compose-dev.yml platform

redeploy-controller:
	cd platform-controller && make build
	docker-compose build
	docker stack deploy --compose-file docker-compose-dev.yml platform


start:
	docker stack deploy --compose-file docker-compose.yml platform

start-dev:
	docker-compose build
	docker stack deploy --compose-file docker-compose.yml platform

build: install-parent-pom
	cd platform-controller && make build
	cd platform-storage/storage-service && mvn clean package -U
	cd analysis-component && mvn clean package -U
	cd hobbit-gui/gui-client && sh -c 'test "$$TRAVIS" = "true" && unlink package-lock.json; true' && npm install --verbose && npm run build-prod
	cd hobbit-gui/gui-serverbackend && mvn clean package

create-networks:
	@docker network inspect hobbit >/dev/null || (docker network create --subnet 172.16.100.0/24 hobbit && echo "Created network: hobbit")
	@docker network inspect hobbit-core >/dev/null || (docker network create --subnet 172.16.101.0/24 hobbit-core && echo "Created network: hobbit-core")
	@docker network inspect hobbit-services >/dev/null || (docker network create --subnet 172.16.102.0/24 hobbit-services && echo "Created network: hobbit-services")

set-keycloak-permissions:
	@chmod --changes 777 config/keycloak
	@chmod --changes 666 config/keycloak/keycloak.h2.db

setup-virtuoso:
	docker-compose up -d virtuoso
	./run-storage-init.sh; true
	docker-compose stop virtuoso
	docker rm vos

install: create-networks set-keycloak-permissions setup-virtuoso
	[ -z "$$DOCKER_HOST" ] && cp --no-clobber docker-compose.override.localhost.yml docker-compose.override.yml; true

run-platform-elk:
	docker stack deploy --compose-file docker-compose-elk.yml elk
	docker stack deploy --compose-file docker-compose.yml platform

test: install-parent-pom
	make --directory=platform-controller test
	cd platform-storage/storage-service && mvn --update-snapshots clean test
	cd analysis-component && mvn --update-snapshots clean test
	cd hobbit-gui/gui-client && sh -c 'test "$$TRAVIS" = "true" && unlink package-lock.json; true' && npm install --verbose && npm run lint
	cd hobbit-gui/gui-serverbackend && mvn --update-snapshots clean test

install-parent-pom:
	cd parent-pom && mvn install

local-controller: lc-build lc-run

lc-build:
	cd platform-controller && make build

lc-run:
	#GITLAB_USER=
	#GITLAB_EMAIL=
	#GITLAB_TOKEN=
	DOCKER_HOST=unix:///var/run/docker.sock \
	HOBBIT_RABBIT_HOST=localhost \
	HOBBIT_REDIS_HOST=localhost \
	DEPLOY_ENV=testing \
	java -cp platform-controller/target/platform-controller.jar \
	org.hobbit.core.run.ComponentStarter \
	org.hobbit.controller.PlatformController

dev:
	docker-compose -f docker-compose-dev.yml build
	docker-compose -f docker-compose-dev.yml -f docker-compose.override.yml up
