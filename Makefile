# =============================================================================
# Backend Portfolio Projects - Makefile
# =============================================================================

.PHONY: all build test clean help

# Default target
all: build

# =============================================================================
# Build & Test (All Projects)
# =============================================================================

## Build all modules (skip tests)
build:
	@echo "🔨 Building all projects..."
	./mvnw clean package -DskipTests -B

## Build with tests
build-test:
	@echo "🔨 Building and testing all projects..."
	./mvnw clean package -B

## Run tests only
test:
	@echo "🧪 Running all tests..."
	./mvnw test -B

## Run tests with coverage
test-coverage:
	@echo "🧪 Running tests with coverage..."
	./mvnw test jacoco:report -B

## Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	./mvnw clean

# =============================================================================
# Individual Project Builds
# =============================================================================

## Build workflow-orchestration-platform
build-workflow:
	@echo "🔨 Building workflow-orchestration-platform..."
	./mvnw -pl workflow-orchestration-platform clean package -DskipTests -B

## Build event-driven-order-platform
build-orders:
	@echo "🔨 Building event-driven-order-platform..."
	./mvnw -pl event-driven-order-platform -am clean package -DskipTests -B

## Build realtime-streaming-analytics
build-streaming:
	@echo "🔨 Building realtime-streaming-analytics..."
	./mvnw -pl realtime-streaming-analytics -am clean package -DskipTests -B

# =============================================================================
# Individual Project Tests
# =============================================================================

## Test workflow-orchestration-platform
test-workflow:
	@echo "🧪 Testing workflow-orchestration-platform..."
	./mvnw -pl workflow-orchestration-platform test -B

## Test event-driven-order-platform
test-orders:
	@echo "🧪 Testing event-driven-order-platform..."
	./mvnw -pl event-driven-order-platform -am test -B

## Test realtime-streaming-analytics
test-streaming:
	@echo "🧪 Testing realtime-streaming-analytics..."
	./mvnw -pl realtime-streaming-analytics -am test -B

# =============================================================================
# Docker Operations
# =============================================================================

## Start all Docker environments
docker-up-all:
	@echo "🐳 Starting all Docker environments..."
	cd workflow-orchestration-platform/docker && docker-compose up -d
	cd event-driven-order-platform/docker && docker-compose up -d
	cd realtime-streaming-analytics/docker && docker-compose up -d

## Stop all Docker environments
docker-down-all:
	@echo "🛑 Stopping all Docker environments..."
	cd workflow-orchestration-platform/docker && docker-compose down
	cd event-driven-order-platform/docker && docker-compose down
	cd realtime-streaming-analytics/docker && docker-compose down

# =============================================================================
# Utilities
# =============================================================================

## Count lines of code
loc:
	@echo "📊 Lines of code:"
	@find . -name "*.java" -not -path "*/target/*" | xargs wc -l | tail -1

## Count test files
test-count:
	@echo "🧪 Test counts:"
	@echo "Workflow: $$(find workflow-orchestration-platform -name "*Test.java" -not -path "*/target/*" | wc -l) tests"
	@echo "Orders: $$(find event-driven-order-platform -name "*Test.java" -not -path "*/target/*" | wc -l) tests"
	@echo "Streaming: $$(find realtime-streaming-analytics -name "*Test.java" -not -path "*/target/*" | wc -l) tests"

# =============================================================================
# Help
# =============================================================================

## Show this help
help:
	@echo ""
	@echo "Backend Portfolio Projects"
	@echo "=========================="
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Build & Test (All):"
	@echo "  build          Build all projects (skip tests)"
	@echo "  build-test     Build with tests"
	@echo "  test           Run all tests"
	@echo "  clean          Clean build artifacts"
	@echo ""
	@echo "Build (Individual):"
	@echo "  build-workflow   Build workflow-orchestration-platform"
	@echo "  build-orders     Build event-driven-order-platform"
	@echo "  build-streaming  Build realtime-streaming-analytics"
	@echo ""
	@echo "Test (Individual):"
	@echo "  test-workflow    Test workflow-orchestration-platform"
	@echo "  test-orders      Test event-driven-order-platform"
	@echo "  test-streaming   Test realtime-streaming-analytics"
	@echo ""
	@echo "Docker:"
	@echo "  docker-up-all    Start all Docker environments"
	@echo "  docker-down-all  Stop all Docker environments"
	@echo ""
	@echo "Projects:"
	@echo "  1. workflow-orchestration-platform"
	@echo "  2. event-driven-order-platform"
	@echo "  3. realtime-streaming-analytics"
	@echo ""
