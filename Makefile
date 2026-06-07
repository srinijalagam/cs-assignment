.PHONY: clean compile test build docker-build docker-up docker-down

ACCOUNT_DIR=account-service
GATEWAY_DIR=gateway-service

clean:
	mvn -f $(ACCOUNT_DIR)/pom.xml clean
	mvn -f $(GATEWAY_DIR)/pom.xml clean

compile:
	mvn -f $(ACCOUNT_DIR)/pom.xml compile
	mvn -f $(GATEWAY_DIR)/pom.xml compile

test:
	mvn -f $(ACCOUNT_DIR)/pom.xml test
	mvn -f $(GATEWAY_DIR)/pom.xml test

build:
	mvn -f $(ACCOUNT_DIR)/pom.xml package -DskipTests
	mvn -f $(GATEWAY_DIR)/pom.xml package -DskipTests

docker-build:
	docker compose build

docker-up:
	docker compose up -d

docker-down:
	docker compose down
