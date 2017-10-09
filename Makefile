# build platform components
build:
	cd platform-controller && make build
	cd platform-storage/storage-service && mvn clean package -U
	cd analysis-component && mvn clean package -U
	cd hobbit-gui/gui-client && npm install && npm run build-prod
	cd hobbit-gui/gui-serverbackend && mvn clean package

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
