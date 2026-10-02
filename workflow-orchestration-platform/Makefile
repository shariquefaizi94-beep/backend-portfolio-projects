.PHONY: help build test integration-test run infra-up infra-down clean

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build the project (skip tests)
	mvn clean package -DskipTests

test: ## Run unit tests
	mvn test

integration-test: ## Run integration tests (requires Docker for Testcontainers)
	mvn verify -DskipUTs

run: ## Run the application locally (requires infra to be up)
	mvn spring-boot:run

infra-up: ## Start infrastructure (Postgres, Redis, Kafka, Prometheus, Grafana)
	docker compose up -d

infra-down: ## Stop infrastructure
	docker compose down

infra-reset: ## Stop infrastructure and delete volumes
	docker compose down -v

clean: ## Clean build artifacts
	mvn clean

# ─── Convenience targets ──────────────────────────
verify: build test ## Build and run all tests

full-test: infra-up test integration-test ## Run all tests including integration

sample-request: ## Send a sample workflow creation request
	curl -s -X POST http://localhost:8080/api/v1/workflows \
		-H "Content-Type: application/json" \
		-d '{"correlationId":"order-$(shell date +%s)","payload":"{\"orderId\":\"ORD-$(shell date +%s)\",\"customerId\":\"CUST-001\",\"totalAmount\":249.99,\"customerEmail\":\"customer@example.com\",\"items\":[{\"sku\":\"LAPTOP-PRO-15\",\"quantity\":1,\"price\":249.99}]}"}' | jq .

stats: ## Get workflow stats
	curl -s http://localhost:8080/api/v1/workflows/stats | jq .
