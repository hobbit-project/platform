# build platform components
build:
	cd platform-controller && make build
	cd platform-storage/storage-service && mvn clean package -U
	cd analysis-component && mvn clean package -U
	cd hobbit-gui/gui-client && npm install && npm run build-prod
	cd hobbit-gui/gui-serverbackend && mvn clean package

install:
	@docker network inspect hobbit >/dev/null || (docker network create hobbit && echo "Created network: hobbit")
	@docker network inspect hobbit-core >/dev/null || (docker network create hobbit-core && echo "Created network: hobbit-core")
	@chmod --changes 777 config/keycloak
	@chmod --changes 666 config/keycloak/keycloak.h2.db

test:
	make --directory=platform-controller test
	cd platform-storage/storage-service && mvn --update-snapshots clean test
	cd analysis-component && mvn --update-snapshots clean test
	cd hobbit-gui/gui-client && npm install && npm run lint
	cd hobbit-gui/gui-serverbackend && mvn --update-snapshots clean test

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
