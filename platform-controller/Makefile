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

test:
	HOBBIT_RABBIT_HOST=localhost \
	mvn test -Dtest=ContainerManagerImplTest

test-all:
	HOBBIT_RABBIT_HOST=localhost \
	HOBBIT_REDIS_HOST=localhost \
	mvn test

deps: redis rabbit

redis:
	docker run -d \
		--name hobbit-redis \
		-p 6379:6379 \
		redis

rediscli:
	docker run -it \
		--link hobbit-redis:redis \
		--rm \
		redis redis-cli -h redis -p 6379

rabbit:
	docker run -d \
		--name hobbit-rabbit \
		-p 5672:5672 \
		-p 8080:15672 \
		rabbitmq:management
