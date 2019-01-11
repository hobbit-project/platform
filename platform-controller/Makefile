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
		target/platform-controller-0.0.1-SNAPSHOT.jar \
		org.hobbit.core.run.ComponentStarter \
		org.hobbit.controller.PlatformController

test: deps
	HOBBIT_RABBIT_HOST=localhost \
	HOBBIT_REDIS_HOST=localhost \
	PROMETHEUS_HOST=localhost \
	mvn --update-snapshots clean test

test-single:
	docker rmi busybox
	mvn --update-snapshots clean test -Dtest=org.hobbit.controller.docker.ContainerManagerImplTest#startContainer

test-all: test

deps: redis rabbit node-exporter cAdvisor prometheus

redis:
	test -n "$$(docker ps -aq --filter name=hobbit-redis)" || \
	docker run -d \
		--name hobbit-redis \
		-p 6379:6379 \
		--rm \
		redis

rediscli:
	docker run -it \
		--link hobbit-redis:redis \
		--rm \
		redis redis-cli -h redis -p 6379

rabbit:
	test -n "$$(docker ps -aq --filter name=hobbit-rabbit)" || \
	docker run -d \
		--name hobbit-rabbit \
		-p 5672:5672 \
		-p 8080:15672 \
		--rm \
		rabbitmq:management

node-exporter:
	test -n "$$(docker ps -aq --filter name=node-exporter)" || \
	docker run -d \
		--name node-exporter \
		-p 9100:9100 \
		--rm \
		prom/node-exporter

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
		google/cadvisor

prometheus:
	test -n "$$(docker ps -aq --filter name=prometheus)" || \
	docker run -d \
		--name prometheus \
		-p 9090:9090 \
		--link cAdvisor:cAdvisor \
		--link node-exporter:node-exporter \
		--rm \
		-v $(shell pwd)/config/prometheus:/config \
		prom/prometheus --config.file=/config/prometheus.conf


