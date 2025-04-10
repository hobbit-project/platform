default: build

install:
	mvn clean install -U -Dmaven.test.skip=true

#build: install
build:
	mvn clean package -U -Dmaven.test.skip=true

run:
	DOCKER_HOST=tcp://localhost:2375 \
	HOBBIT_RABBIT_HOST=localhost \
	java -cp \
		target/platform-controller.jar \
		org.hobbit.core.run.ComponentStarter \
		org.hobbit.controller.PlatformController

test:
	@echo "🔧 Starting required services with Docker Compose..."
	docker compose --file=../docker-compose-dev.yml up -d cadvisor node-exporter prometheus redis rabbit
	@export HOBBIT_RABBIT_IMAGE=rabbitmq:management && \
	export HOBBIT_RABBIT_HOST=localhost && \
	export HOBBIT_RABBIT_EXPERIMENTS_HOST=localhost && \
	export HOBBIT_REDIS_HOST=localhost && \
	export MAX_EXECUTION_TIME=1200000 && \
	export PROMETHEUS_HOST=localhost && \
	mvn --quiet --update-snapshots -Dtest=$(test) clean test
	@echo "✅ Tests completed successfully."


test-single:
	docker rmi busybox
	mvn --update-snapshots clean test -Dtest=org.hobbit.controller.docker.ContainerManagerImplTest#startContainer

test-all: test

rediscli:
	docker run -it \
		--network hobbit-core \
		--rm \
		redis redis-cli -h redis -p 6379

cAdvisor:
	test -n "$$(docker ps -aq --filter name=cadvisor)" || \
	docker run -d \
		--name cadvisor \
		-p 8081:8080 \
		--rm \
		-v /:/rootfs:ro \
		-v /var/run:/var/run:rw \
		-v /sys:/sys:ro \
		-v /var/lib/docker/:/var/lib/docker:ro \
		-v /dev/disk:/dev/disk:ro \
		gcr.io/cadvisor/cadvisor

trigger-all-correlation-analysis:
	docker run --network hobbit-core --volume $(CURDIR):/data:ro -e "HOBBIT_RABBIT_HOST=rabbit" java:alpine /data/test_trigger_all_correlation_analysis.sh
