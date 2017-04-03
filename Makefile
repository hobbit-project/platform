# build platform components
build:
	cd hobbit-gui/gui-client && npm install && npm run build
    mvn clean package -U -DskipTests
	#cd platform-controller && make build
	#cd platform-storage/storage-service && mvn clean package -U
	#cd analysis-component && mvn clean package -U
	#cd hobbit-gui/gui-serverbackend && mvn package
