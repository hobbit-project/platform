# build platform components
build:
	cd platform-controller && make build
	cd platform-storage/storage-service && mvn clean package -U
	cd hobbit-gui/gui-client && npm install && npm run build
	cd hobbit-gui/gui-serverbackend && mvn package
