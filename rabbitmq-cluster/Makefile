haproxy=$(shell docker ps -a | grep haproxy | cut -d' ' -f 1) 

start:
	docker-compose up -d

restart-haproxy:
	docker stop ${haproxy} && docker rm ${haproxy}
	docker-compose up -d

stop:
	docker-compose down
