build: build-dev-images

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

build-dev-images: build-dev-platform-controller-image build-dev-gui-image build-dev-analysis-image build-dev-storage-image

build-dev-platform-controller-image:
	docker build -t hobbitproject/hobbit-platform-controller:dev --file platform-controller/Dockerfile .

build-dev-gui-image:
	docker build -t hobbitproject/hobbit-gui:dev --file hobbit-gui/gui-serverbackend/Dockerfile .

build-dev-analysis-image:
	docker build -t hobbitproject/hobbit-analysis-component:dev --file ./analysis-component/Dockerfile .

build-dev-storage-image:
	docker build -t hobbitproject/hobbit-storage-service:dev --file ./platform-storage/storage-service/Dockerfile .

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
	
version=2.0.17
minor=2.0
major=2

controllerImage=hobbitproject/hobbit-platform-controller
storageImage=hobbitproject/hobbit-storage-service
guiImage=hobbitproject/hobbit-gui
analysisImage=hobbitproject/hobbit-analysis-component

# Build dev images and tag them
build-images: build
	docker tag hobbitproject/hobbit-platform-controller:dev git.project-hobbit.eu:4567/gitadmin/platform-controller-image
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-controller-image git.project-hobbit.eu:4567/gitadmin/platform-controller-image:$(version)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-controller-image git.project-hobbit.eu:4567/gitadmin/platform-controller-image:$(minor)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-controller-image git.project-hobbit.eu:4567/gitadmin/platform-controller-image:$(major)
	docker tag hobbitproject/hobbit-storage-service:dev git.project-hobbit.eu:4567/gitadmin/platform-storage-image
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-storage-image git.project-hobbit.eu:4567/gitadmin/platform-storage-image:$(version)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-storage-image git.project-hobbit.eu:4567/gitadmin/platform-storage-image:$(minor)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-storage-image git.project-hobbit.eu:4567/gitadmin/platform-storage-image:$(major)
	docker tag hobbitproject/hobbit-gui:dev git.project-hobbit.eu:4567/gitadmin/platform-gui-image
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-gui-image git.project-hobbit.eu:4567/gitadmin/platform-gui-image:$(version)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-gui-image git.project-hobbit.eu:4567/gitadmin/platform-gui-image:$(minor)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-gui-image git.project-hobbit.eu:4567/gitadmin/platform-gui-image:$(major)
	docker tag hobbitproject/hobbit-analysis-component:dev git.project-hobbit.eu:4567/gitadmin/platform-analysis-image
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-analysis-image git.project-hobbit.eu:4567/gitadmin/platform-analysis-image:$(version)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-analysis-image git.project-hobbit.eu:4567/gitadmin/platform-analysis-image:$(minor)
	docker tag git.project-hobbit.eu:4567/gitadmin/platform-analysis-image git.project-hobbit.eu:4567/gitadmin/platform-analysis-image:$(major)

# create and push docker images for deployment
push: build-images
	docker push git.project-hobbit.eu:4567/gitadmin/platform-controller-image
	docker push git.project-hobbit.eu:4567/gitadmin/platform-storage-image
	docker push git.project-hobbit.eu:4567/gitadmin/platform-gui-image
	docker push git.project-hobbit.eu:4567/gitadmin/platform-analysis-image

build-images-docker-hub: build-images
	docker tag hobbitproject/hobbit-platform-controller:dev $(controllerImage)
	docker tag $(controllerImage) $(controllerImage):$(version)
	docker tag hobbitproject/hobbit-storage-service:dev $(storageImage)
	docker tag $(storageImage) $(storageImage):$(version)
	docker tag hobbitproject/hobbit-gui:dev $(guiImage)
	docker tag $(guiImage) $(guiImage):$(version) 
	docker tag hobbitproject/hobbit-analysis-component:dev $(analysisImage)
	docker tag $(analysisImage) $(analysisImage):$(version)

# create and push docker images for deployment
push-docker-hub: 
	docker login
	docker push $(controllerImage):latest
	docker push $(storageImage):latest
	docker push $(guiImage):latest
	docker push $(analysisImage):latest
	docker push $(controllerImage):$(version)
	docker push $(storageImage):$(version)
	docker push $(guiImage):$(version)
	docker push $(analysisImage):$(version)

