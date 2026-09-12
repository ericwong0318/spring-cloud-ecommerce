.PHONY: build build-all up down logs clean test verify help infra

SERVICES = config-server eureka-server gateway product category auth-server order-service inventory-service notification-service payment-service

INFRA = mongodb kafka zookeeper kafka-connect otel-collector rabbitmq postgres-product postgres-category postgres-order postgres-payment postgres-inventory postgres-notification

build:
	docker compose build $(filter-out $@,$(MAKECMDGOALS))

build-all:
	docker compose build

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

infra-up:
	docker compose up -d $(INFRA)

infra-down:
	docker compose down

infra-logs:
	docker compose logs -f

test:
	mvn test

verify:
	mvn verify -Dskip.unit.tests=true

clean:
	mvn clean
	docker system prune -f

help:
	@echo "Available targets:"
	@echo "  build [service]       - Build specific service or all services"
	@echo "  build-all             - Build all services"
	@echo "  up                    - Start all services via docker-compose"
	@echo "  down                  - Stop all services"
	@echo "  logs                  - Follow docker-compose logs"
	@echo "  infra-up              - Start infrastructure (db, kafka, rabbitmq, otel)"
	@echo "  infra-down            - Stop infrastructure"
	@echo "  infra-logs            - Follow infrastructure logs"
	@echo "  test                  - Run unit tests"
	@echo "  verify                - Run integration tests"
	@echo "  clean                 - Clean Maven + Docker"
	@echo "  help                  - List all targets"

%:
	@: