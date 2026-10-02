package com.portfolio.observability.metrics.service;

import com.portfolio.observability.metrics.model.MetricDataPoint;
import com.portfolio.observability.metrics.model.MetricDefinition;
import com.portfolio.observability.metrics.model.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetricsService Tests")
class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Nested
    @DisplayName("Metric Definition Tests")
    class MetricDefinitionTests {

        @Test
        @DisplayName("Should register a metric definition")
        void shouldRegisterMetricDefinition() {
            MetricDefinition definition = MetricDefinition.counter(
                    "http_requests_total", "Total HTTP requests", List.of("method", "path"));

            MetricDefinition registered = metricsService.registerMetric(definition);

            assertThat(registered).isNotNull();
            assertThat(registered.name()).isEqualTo("http_requests_total");
            assertThat(registered.type()).isEqualTo(MetricType.COUNTER);
        }

        @Test
        @DisplayName("Should retrieve registered metric definition")
        void shouldRetrieveMetricDefinition() {
            MetricDefinition definition = MetricDefinition.gauge(
                    "cpu_usage", "CPU usage percentage", "percent");
            metricsService.registerMetric(definition);

            var retrieved = metricsService.getMetricDefinition("cpu_usage");

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().name()).isEqualTo("cpu_usage");
            assertThat(retrieved.get().type()).isEqualTo(MetricType.GAUGE);
        }

        @Test
        @DisplayName("Should return empty for non-existent metric")
        void shouldReturnEmptyForNonExistentMetric() {
            var result = metricsService.getMetricDefinition("non_existent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get all metric definitions")
        void shouldGetAllMetricDefinitions() {
            metricsService.registerMetric(MetricDefinition.counter("metric1", "desc1", List.of()));
            metricsService.registerMetric(MetricDefinition.gauge("metric2", "desc2", "unit"));

            List<MetricDefinition> all = metricsService.getAllMetricDefinitions();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("Should get metrics by type")
        void shouldGetMetricsByType() {
            metricsService.registerMetric(MetricDefinition.counter("counter1", "desc1", List.of()));
            metricsService.registerMetric(MetricDefinition.counter("counter2", "desc2", List.of()));
            metricsService.registerMetric(MetricDefinition.gauge("gauge1", "desc3", "unit"));

            List<MetricDefinition> counters = metricsService.getMetricsByType(MetricType.COUNTER);

            assertThat(counters).hasSize(2);
            assertThat(counters).allMatch(d -> d.type() == MetricType.COUNTER);
        }

        @Test
        @DisplayName("Should unregister metric definition")
        void shouldUnregisterMetricDefinition() {
            metricsService.registerMetric(MetricDefinition.counter("to_delete", "desc", List.of()));
            assertThat(metricsService.isMetricRegistered("to_delete")).isTrue();

            metricsService.unregisterMetric("to_delete");

            assertThat(metricsService.isMetricRegistered("to_delete")).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Point Recording Tests")
    class DataPointRecordingTests {

        @Test
        @DisplayName("Should record a data point")
        void shouldRecordDataPoint() {
            MetricDataPoint dataPoint = MetricDataPoint.of("test_metric", 42.0);

            MetricDataPoint recorded = metricsService.recordDataPoint(dataPoint);

            assertThat(recorded).isNotNull();
            assertThat(recorded.metricName()).isEqualTo("test_metric");
            assertThat(recorded.value()).isEqualTo(42.0);
        }

        @Test
        @DisplayName("Should record data point with labels")
        void shouldRecordDataPointWithLabels() {
            Map<String, String> labels = Map.of("method", "GET", "path", "/api/users");
            MetricDataPoint dataPoint = MetricDataPoint.of("http_requests", 100.0, labels);

            MetricDataPoint recorded = metricsService.recordDataPoint(dataPoint);

            assertThat(recorded.labels()).containsEntry("method", "GET");
            assertThat(recorded.labels()).containsEntry("path", "/api/users");
        }

        @Test
        @DisplayName("Should record value directly")
        void shouldRecordValueDirectly() {
            MetricDataPoint recorded = metricsService.recordValue("direct_metric", 99.9);

            assertThat(recorded.metricName()).isEqualTo("direct_metric");
            assertThat(recorded.value()).isEqualTo(99.9);
        }

        @Test
        @DisplayName("Should record multiple data points in batch")
        void shouldRecordMultipleDataPointsInBatch() {
            List<MetricDataPoint> dataPoints = List.of(
                    MetricDataPoint.of("metric1", 1.0),
                    MetricDataPoint.of("metric2", 2.0),
                    MetricDataPoint.of("metric3", 3.0)
            );

            List<MetricDataPoint> recorded = metricsService.recordDataPoints(dataPoints);

            assertThat(recorded).hasSize(3);
        }

        @Test
        @DisplayName("Should increment counter")
        void shouldIncrementCounter() {
            metricsService.recordValue("counter", 10.0);
            metricsService.incrementCounter("counter", 5.0, Map.of());

            var latest = metricsService.getLatestDataPoint("counter");

            assertThat(latest).isPresent();
            assertThat(latest.get().value()).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("Data Point Querying Tests")
    class DataPointQueryingTests {

        @BeforeEach
        void setUpDataPoints() {
            for (int i = 0; i < 10; i++) {
                metricsService.recordValue("test_metric", i * 10.0);
            }
        }

        @Test
        @DisplayName("Should get all data points for a metric")
        void shouldGetAllDataPoints() {
            List<MetricDataPoint> dataPoints = metricsService.getDataPoints("test_metric");

            assertThat(dataPoints).hasSize(10);
        }

        @Test
        @DisplayName("Should get latest data point")
        void shouldGetLatestDataPoint() {
            var latest = metricsService.getLatestDataPoint("test_metric");

            assertThat(latest).isPresent();
            assertThat(latest.get().value()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should get latest values for all metrics")
        void shouldGetLatestValuesForAllMetrics() {
            metricsService.recordValue("another_metric", 55.0);

            Map<String, Double> latestValues = metricsService.getLatestValues();

            assertThat(latestValues).containsKeys("test_metric", "another_metric");
        }

        @Test
        @DisplayName("Should get data points in time range")
        void shouldGetDataPointsInTimeRange() {
            Instant start = Instant.now().minusSeconds(60);
            Instant end = Instant.now().plusSeconds(60);

            List<MetricDataPoint> inRange = metricsService.getDataPointsInRange(
                    "test_metric", start, end);

            assertThat(inRange).hasSize(10);
        }
    }

    @Nested
    @DisplayName("Aggregation Tests")
    class AggregationTests {

        @BeforeEach
        void setUpDataPoints() {
            // Record values 10, 20, 30, 40, 50
            for (int i = 1; i <= 5; i++) {
                metricsService.recordValue("agg_metric", i * 10.0);
            }
        }

        @Test
        @DisplayName("Should calculate average")
        void shouldCalculateAverage() {
            double avg = metricsService.calculateAverage("agg_metric", Duration.ofHours(1));

            assertThat(avg).isEqualTo(30.0); // (10+20+30+40+50)/5
        }

        @Test
        @DisplayName("Should calculate sum")
        void shouldCalculateSum() {
            double sum = metricsService.calculateSum("agg_metric", Duration.ofHours(1));

            assertThat(sum).isEqualTo(150.0); // 10+20+30+40+50
        }

        @Test
        @DisplayName("Should calculate min")
        void shouldCalculateMin() {
            double min = metricsService.calculateMin("agg_metric", Duration.ofHours(1));

            assertThat(min).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should calculate max")
        void shouldCalculateMax() {
            double max = metricsService.calculateMax("agg_metric", Duration.ofHours(1));

            assertThat(max).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate percentile")
        void shouldCalculatePercentile() {
            double p50 = metricsService.calculatePercentile("agg_metric", Duration.ofHours(1), 50);

            assertThat(p50).isGreaterThanOrEqualTo(20.0);
            assertThat(p50).isLessThanOrEqualTo(40.0);
        }

        @Test
        @DisplayName("Should calculate statistics")
        void shouldCalculateStatistics() {
            var stats = metricsService.calculateStatistics("agg_metric", Duration.ofHours(1));

            assertThat(stats.metricName()).isEqualTo("agg_metric");
            assertThat(stats.count()).isEqualTo(5);
            assertThat(stats.sum()).isEqualTo(150.0);
            assertThat(stats.average()).isEqualTo(30.0);
            assertThat(stats.min()).isEqualTo(10.0);
            assertThat(stats.max()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("Maintenance Tests")
    class MaintenanceTests {

        @Test
        @DisplayName("Should count data points")
        void shouldCountDataPoints() {
            for (int i = 0; i < 5; i++) {
                metricsService.recordValue("count_test", i);
            }

            long count = metricsService.getDataPointCount("count_test");

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("Should get total data point count")
        void shouldGetTotalDataPointCount() {
            metricsService.recordValue("metric1", 1.0);
            metricsService.recordValue("metric2", 2.0);
            metricsService.recordValue("metric2", 3.0);

            long total = metricsService.getTotalDataPointCount();

            assertThat(total).isEqualTo(3);
        }

        @Test
        @DisplayName("Should clear all data")
        void shouldClearAllData() {
            metricsService.registerMetric(MetricDefinition.counter("test", "desc", List.of()));
            metricsService.recordValue("test", 100.0);

            metricsService.clear();

            assertThat(metricsService.getAllMetricDefinitions()).isEmpty();
            assertThat(metricsService.getTotalDataPointCount()).isZero();
        }
    }
}
