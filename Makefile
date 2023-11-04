# build platform components
default: build

deploy: create-networks start

redeploy: build build-dev-images start-dev-platform

redeploy-gui: install-parent-pom build-gui build-dev-gui-image remove-gui-service start-dev-platform

remove-gui-service:
	docker service rm platform_gui

start:
	docker stack deploy --compose-file docker-compose.yml platform

start-dev: start-rabbitmq-cluster start-dev-platform

start-dev-elk: start-rabbitmq-cluster start-dev-platform start-dev-elk

start-rabbitmq-cluster:
	cd rabbitmq-cluster && make start

start-dev-platform:
	docker-compose -f docker-compose-dev.yml up -d

start-dev-elk:
	docker-compose -f docker-compose-elk.yml up -d

build: build-java build-dev-images

build-java: install-parent-pom build-controller build-storage build-analysis build-gui

build-gui:
	# see hobbit-gui/gui-serverbackend/Dockerfile

build-controller:
	cd platform-controller && make build

build-storage:
	cd platform-storage/storage-service && mvn clean package -U

build-analysis:
	cd analysis-component && mvn clean package -U

build-dev-images: build-dev-platform-controller-image build-dev-gui-image build-dev-analysis-image build-dev-storage-image

build-dev-platform-controller-image:
	docker build -t hobbitproject/hobbit-platform-controller:dev ./platform-controller

build-dev-gui-image:
	docker build -t hobbitproject/hobbit-gui:dev --file hobbit-gui/gui-serverbackend/Dockerfile .

build-dev-analysis-image:
	docker build -t hobbitproject/hobbit-analysis-component:dev ./analysis-component

build-dev-storage-image:
	docker build -t hobbitproject/hobbit-storage-service:dev ./platform-storage/storage-service

create-networks:
	@docker network inspect hobbit >/dev/null || (docker network create -d overlay --attachable --subnet 172.16.100.0/24 hobbit && echo "Created network: hobbit")
	@docker network inspect hobbit-core >/dev/null || (docker network create -d overlay --attachable --subnet 172.16.101.0/24 hobbit-core && echo "Created network: hobbit-core")
	@docker network inspect hobbit-services >/dev/null || (docker network create -d overlay --attachable --subnet 172.16.102.0/24 hobbit-services && echo "Created network: hobbit-services")

set-keycloak-permissions:
	@chmod --changes 777 config/keycloak
	@chmod --changes 666 config/keycloak/keycloak.h2.db

setup-virtuoso:
	docker-compose up -d vos
	./run-storage-init.sh; true
	docker-compose stop vos
	docker rm vos

install: create-networks set-keycloak-permissions setup-virtuoso
	[ -z "$$DOCKER_HOST" ] && cp --no-clobber docker-compose.override.localhost.yml docker-compose.override.yml; true

run-platform-elk:
	docker stack deploy --compose-file docker-compose-elk.yml elk
	docker stack deploy --compose-file docker-compose.yml platform

test: create-networks install-parent-pom
	make --directory=platform-controller test
	cd platform-storage/storage-service && mvn --quiet --update-snapshots clean test
	cd analysis-component && mvn --quiet --update-snapshots clean test
	cd hobbit-gui/gui-client && sh -c 'test "$$TRAVIS" = "true" && npm --quiet ci; true' && sh -c 'test "$$TRAVIS" = "true" || npm --quiet install; true' && npm --quiet run lint && npm --quiet run build-prod
	cd hobbit-gui/gui-serverbackend && mvn --quiet --update-snapshots clean test

install-parent-pom:
	cd parent-pom && mvn --quiet install

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

clean:
	cd analysis-component && mvn clean
	cd platform-controller && mvn clean
	cd platform-storage/storage-service && mvn clean
	cd hobbit-gui/gui-serverbackend && mvn clean
