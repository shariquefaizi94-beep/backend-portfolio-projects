# Deployment Guide

## Overview

This guide covers deploying the Real-Time Streaming Analytics Platform in various environments, from local development to production Kubernetes clusters.

## Local Development

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Quick Start

```bash
# Clone repository
git clone https://github.com/shariquefaizi94-beep/realtime-streaming-analytics.git
cd realtime-streaming-analytics

# Build all modules
./mvnw clean package -DskipTests

# Start infrastructure
cd docker && docker-compose up -d kafka redis elasticsearch prometheus grafana

# Wait for services to be healthy (about 30 seconds)
docker-compose ps

# Start applications
docker-compose up -d event-producer stream-processor analytics-api

# Verify
curl http://localhost:8080/actuator/health
```

### Running Services Locally (Without Docker)

For development with hot reload:

```bash
# Terminal 1: Start infrastructure only
cd docker && docker-compose up -d kafka redis elasticsearch

# Terminal 2: Run Event Producer
./mvnw -pl event-producer spring-boot:run

# Terminal 3: Run Stream Processor
./mvnw -pl stream-processor spring-boot:run

# Terminal 4: Run Analytics API
./mvnw -pl analytics-api spring-boot:run
```

---

## Docker Deployment

### Build Images

```bash
# Build all modules
./mvnw clean package -DskipTests

# Build Docker images
cd docker
docker-compose build
```

### Environment Configuration

Create `.env` file in the `docker/` directory:

```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPICS_CLICK_EVENTS=click-events

# Redis
SPRING_DATA_REDIS_HOST=redis

# Elasticsearch
ELASTICSEARCH_URL=http://elasticsearch:9200

# Generator Config
GENERATOR_EVENTS_PER_BATCH=100
GENERATOR_INTERVAL_MS=100

# Aggregation Config
AGGREGATION_WINDOW_SIZE_SECONDS=60
AGGREGATION_FLUSH_INTERVAL_MS=60000
```

### Docker Compose Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f stream-processor

# Stop services (preserve data)
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Restart single service
docker-compose restart analytics-api

# Scale service (if configured for scaling)
docker-compose up -d --scale stream-processor=3
```

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.25+)
- kubectl configured
- Helm 3.x (optional, for infrastructure)

### Install Infrastructure with Helm

```bash
# Add Helm repositories
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add elastic https://helm.elastic.co
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Kafka (KRaft mode)
helm install kafka bitnami/kafka \
  --set kraft.enabled=true \
  --set zookeeper.enabled=false \
  --set replicaCount=3 \
  --namespace streaming --create-namespace

# Install Redis
helm install redis bitnami/redis \
  --set auth.enabled=false \
  --set master.persistence.size=1Gi \
  --namespace streaming

# Install Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --set replicas=3 \
  --set minimumMasterNodes=2 \
  --set persistence.size=10Gi \
  --namespace streaming

# Install Prometheus Stack
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace
```

### Kubernetes Manifests

Create `k8s/` directory with the following:

**k8s/namespace.yaml**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: streaming-analytics
```

**k8s/configmap.yaml**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: streaming-config
  namespace: streaming-analytics
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka.streaming.svc.cluster.local:9092"
  SPRING_DATA_REDIS_HOST: "redis-master.streaming.svc.cluster.local"
  ELASTICSEARCH_URL: "http://elasticsearch-master.streaming.svc.cluster.local:9200"
  GENERATOR_EVENTS_PER_BATCH: "100"
  AGGREGATION_WINDOW_SIZE_SECONDS: "60"
```

**k8s/analytics-api.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analytics-api
  namespace: streaming-analytics
spec:
  replicas: 2
  selector:
    matchLabels:
      app: analytics-api
  template:
    metadata:
      labels:
        app: analytics-api
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: analytics-api
          image: analytics-api:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: streaming-config
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: analytics-api
  namespace: streaming-analytics
spec:
  selector:
    app: analytics-api
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

### Deploy to Kubernetes

```bash
# Apply manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/analytics-api.yaml
kubectl apply -f k8s/stream-processor.yaml
kubectl apply -f k8s/event-producer.yaml

# Check status
kubectl get pods -n streaming-analytics
kubectl get services -n streaming-analytics

# View logs
kubectl logs -f deployment/analytics-api -n streaming-analytics
```

---

## Production Considerations

### High Availability

| Component | HA Strategy |
|-----------|-------------|
| Kafka | 3+ brokers, replication factor 3 |
| Redis | Redis Sentinel or Cluster mode |
| Elasticsearch | 3+ nodes, replicated indices |
| Applications | 2+ replicas with anti-affinity |

### Resource Recommendations

**Event Producer**
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Stream Processor**
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "2000m"
```

**Analytics API**
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### Security Checklist

- [ ] Enable TLS for all external endpoints
- [ ] Configure Kafka SASL/SCRAM authentication
- [ ] Enable Redis AUTH
- [ ] Set Elasticsearch API keys and RBAC
- [ ] Add API authentication (OAuth2/JWT)
- [ ] Configure network policies
- [ ] Enable secrets management (Vault, AWS Secrets Manager)
- [ ] Set up WAF for public endpoints

### Monitoring Checklist

- [ ] Prometheus scraping all services
- [ ] Grafana dashboards imported
- [ ] Alerting rules configured
- [ ] Log aggregation (ELK/Loki)
- [ ] Distributed tracing (Jaeger/Zipkin)

---

## Troubleshooting

### Common Issues

**Kafka Connection Refused**
```bash
# Check Kafka is running
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Verify network connectivity
docker exec stream-processor ping kafka
```

**Elasticsearch Yellow/Red Status**
```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# Check indices
curl http://localhost:9200/_cat/indices?v
```

**Redis Connection Issues**
```bash
# Test Redis connection
redis-cli -h localhost ping

# Check Redis logs
docker logs redis
```

**High Memory Usage in Stream Processor**
- Increase JVM heap: `-Xmx2g`
- Reduce window size
- Decrease batch size
- Add more consumer instances

### Viewing Logs

```bash
# Docker Compose
docker-compose logs -f --tail=100 stream-processor

# Kubernetes
kubectl logs -f deployment/stream-processor -n streaming-analytics --tail=100

# Search for errors
docker-compose logs | grep -i error
```

### Metrics Debugging

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Query specific metric
curl 'http://localhost:9090/api/v1/query?query=kafka_events_processed_total'

# Check actuator metrics
curl http://localhost:8082/actuator/prometheus | grep kafka
```
