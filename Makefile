# build platform components
build:
	cd hobbit-gui/gui-client && npm install && npm run build
	mvn clean package -U -DskipTests
