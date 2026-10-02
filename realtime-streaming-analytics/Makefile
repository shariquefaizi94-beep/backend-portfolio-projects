# =============================================================================
# Real-Time Streaming Analytics Platform - Makefile
# =============================================================================

.PHONY: all build test clean docker-build docker-up docker-down docker-logs \
        start-infra stop-infra start-services stop-services help

# Default target
all: build

# =============================================================================
# Build & Test
# =============================================================================

## Build all modules (skip tests)
build:
	@echo "🔨 Building all modules..."
	./mvnw clean package -DskipTests

## Build with tests
build-test:
	@echo "🔨 Building and testing all modules..."
	./mvnw clean package

## Run tests only
test:
	@echo "🧪 Running tests..."
	./mvnw test

## Run tests with coverage
test-coverage:
	@echo "🧪 Running tests with coverage..."
	./mvnw test jacoco:report

## Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	./mvnw clean

## Format code (if spotless configured)
format:
	@echo "✨ Formatting code..."
	./mvnw spotless:apply || echo "Spotless not configured"

## Check code style
lint:
	@echo "🔍 Checking code style..."
	./mvnw spotless:check || echo "Spotless not configured"

# =============================================================================
# Docker Operations
# =============================================================================

## Build Docker images for all services
docker-build: build
	@echo "🐳 Building Docker images..."
	cd docker && docker-compose build

## Start all services (infra + apps)
docker-up:
	@echo "🚀 Starting all services..."
	cd docker && docker-compose up -d

## Stop all services
docker-down:
	@echo "🛑 Stopping all services..."
	cd docker && docker-compose down

## Stop and remove volumes
docker-clean:
	@echo "🧹 Stopping services and removing volumes..."
	cd docker && docker-compose down -v

## View logs
docker-logs:
	@echo "📜 Viewing logs..."
	cd docker && docker-compose logs -f

## View logs for specific service
docker-logs-%:
	cd docker && docker-compose logs -f $*

# =============================================================================
# Infrastructure Only
# =============================================================================

## Start infrastructure only (Kafka, Redis, ES, Prometheus, Grafana)
start-infra:
	@echo "🏗️  Starting infrastructure..."
	cd docker && docker-compose up -d kafka redis elasticsearch kibana prometheus grafana

## Stop infrastructure
stop-infra:
	@echo "🛑 Stopping infrastructure..."
	cd docker && docker-compose stop kafka redis elasticsearch kibana prometheus grafana

# =============================================================================
# Application Services Only
# =============================================================================

## Start application services (requires infra running)
start-services:
	@echo "🚀 Starting application services..."
	cd docker && docker-compose up -d event-producer stream-processor analytics-api

## Stop application services
stop-services:
	@echo "🛑 Stopping application services..."
	cd docker && docker-compose stop event-producer stream-processor analytics-api

# =============================================================================
# Local Development
# =============================================================================

## Run event-producer locally (requires infra)
run-producer:
	@echo "🚀 Starting Event Producer..."
	./mvnw -pl event-producer spring-boot:run

## Run stream-processor locally (requires infra)
run-processor:
	@echo "🚀 Starting Stream Processor..."
	./mvnw -pl stream-processor spring-boot:run

## Run analytics-api locally (requires infra)
run-api:
	@echo "🚀 Starting Analytics API..."
	./mvnw -pl analytics-api spring-boot:run

# =============================================================================
# Utilities
# =============================================================================

## Create Kafka topic
create-topic:
	@echo "📝 Creating click-events topic..."
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --topic click-events --partitions 6 --replication-factor 1 \
		--if-not-exists

## List Kafka topics
list-topics:
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

## Check service health
health-check:
	@echo "🏥 Checking service health..."
	@echo "\n--- Kafka ---"
	@curl -s http://localhost:8081/actuator/health 2>/dev/null | head -1 || echo "Event Producer: Not running"
	@echo "\n--- Stream Processor ---"
	@curl -s http://localhost:8082/actuator/health 2>/dev/null | head -1 || echo "Stream Processor: Not running"
	@echo "\n--- Analytics API ---"
	@curl -s http://localhost:8080/actuator/health 2>/dev/null | head -1 || echo "Analytics API: Not running"
	@echo "\n--- Elasticsearch ---"
	@curl -s http://localhost:9200/_cluster/health 2>/dev/null | head -1 || echo "Elasticsearch: Not running"
	@echo "\n--- Redis ---"
	@redis-cli ping 2>/dev/null || echo "Redis: Not running"

# =============================================================================
# Demo
# =============================================================================

## Start event generation
start-generator:
	@echo "▶️  Starting event generator..."
	curl -X POST http://localhost:8081/api/v1/generator/start

## Stop event generation
stop-generator:
	@echo "⏹️  Stopping event generator..."
	curl -X POST http://localhost:8081/api/v1/generator/stop

## Check generator status
generator-status:
	@curl -s http://localhost:8081/api/v1/generator/status | jq .

## Get dashboard summary
dashboard:
	@curl -s http://localhost:8080/api/v1/analytics/dashboard | jq .

# =============================================================================
# Help
# =============================================================================

## Show this help
help:
	@echo ""
	@echo "Real-Time Streaming Analytics Platform"
	@echo "======================================"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Build & Test:"
	@echo "  build          Build all modules (skip tests)"
	@echo "  build-test     Build with tests"
	@echo "  test           Run tests only"
	@echo "  clean          Clean build artifacts"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build   Build Docker images"
	@echo "  docker-up      Start all services"
	@echo "  docker-down    Stop all services"
	@echo "  docker-clean   Stop and remove volumes"
	@echo "  docker-logs    View all logs"
	@echo ""
	@echo "Infrastructure:"
	@echo "  start-infra    Start Kafka, Redis, ES, Prometheus, Grafana"
	@echo "  stop-infra     Stop infrastructure"
	@echo ""
	@echo "Services:"
	@echo "  start-services Start app services (needs infra)"
	@echo "  stop-services  Stop app services"
	@echo ""
	@echo "Local Dev:"
	@echo "  run-producer   Run event-producer locally"
	@echo "  run-processor  Run stream-processor locally"
	@echo "  run-api        Run analytics-api locally"
	@echo ""
	@echo "Demo:"
	@echo "  start-generator Start event generation"
	@echo "  stop-generator  Stop event generation"
	@echo "  dashboard       Get dashboard summary"
	@echo ""
	@echo "URLs:"
	@echo "  Analytics API:  http://localhost:8080/swagger-ui.html"
	@echo "  Event Producer: http://localhost:8081/actuator"
	@echo "  Stream Processor: http://localhost:8082/actuator"
	@echo "  Grafana:        http://localhost:3000 (admin/streaming123)"
	@echo "  Prometheus:     http://localhost:9090"
	@echo "  Kibana:         http://localhost:5601"
	@echo "  Elasticsearch:  http://localhost:9200"
	@echo ""
