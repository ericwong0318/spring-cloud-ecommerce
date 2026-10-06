.PHONY: build build-all images k8s-apply k8s-delete up down logs clean test verify help infra

SERVICES = config-server eureka-server gateway product category auth-server order-service inventory-service notification-service payment-service

INFRA = mongodb kafka zookeeper kafka-connect otel-collector rabbitmq postgres-auth postgres-category postgres-order postgres-payment postgres-inventory postgres-notification

# --- Compose path: local dev stack built from the per-service Dockerfiles ---
build:
	docker compose build $(filter-out $@,$(MAKECMDGOALS))

build-all:
	docker compose build

# --- Buildpack path: canonical <service>:1.0.0 images consumed by every
#     Kubernetes packaging path (k8s/deployments.yaml, k8s/base, k8s/helm) ---
images:
	mvn spring-boot:build-image -Pdocker -DskipTests

# --- Kubernetes path: raw manifests for the local OrbStack cluster ---
k8s-apply:
	kubectl apply -f k8s/deployments.yaml

k8s-delete:
	kubectl delete -f k8s/deployments.yaml

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
	@echo "  build [service]       - Build specific service or all services (docker compose / Dockerfiles)"
	@echo "  build-all             - Build all services (docker compose / Dockerfiles)"
	@echo "  images                - Build <service>:1.0.0 images via buildpacks (canonical, for k8s)"
	@echo "  k8s-apply             - Apply k8s/deployments.yaml to the cluster"
	@echo "  k8s-delete            - Remove resources applied from k8s/deployments.yaml"
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