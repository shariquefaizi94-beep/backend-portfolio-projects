.PHONY: help build test run-all infra-up infra-down clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build all modules
	mvn clean package -DskipTests

test: ## Run all unit tests
	mvn test

infra-up: ## Start infrastructure (Postgres, Kafka, Prometheus, Grafana)
	docker compose up -d

infra-down: ## Stop infrastructure
	docker compose down

infra-reset: ## Stop infrastructure and delete volumes
	docker compose down -v

clean: ## Clean build artifacts
	mvn clean

sample-order: ## Send a sample order
	curl -s -X POST http://localhost:8081/api/v1/orders \
		-H "Content-Type: application/json" \
		-d '{"idempotencyKey":"order-$(shell date +%s)","customerId":"CUST-001","totalAmount":149.99,"currency":"USD","items":[{"sku":"LAPTOP-PRO","quantity":1,"price":149.99}]}' | jq .
