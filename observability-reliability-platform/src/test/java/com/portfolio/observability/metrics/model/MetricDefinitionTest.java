package com.portfolio.observability.metrics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetricDefinition Tests")
class MetricDefinitionTest {

    @Test
    @DisplayName("Should create counter metric")
    void shouldCreateCounterMetric() {
        MetricDefinition counter = MetricDefinition.counter(
                "http_requests_total", "Total HTTP requests", List.of("method", "path"));

        assertThat(counter.name()).isEqualTo("http_requests_total");
        assertThat(counter.type()).isEqualTo(MetricType.COUNTER);
        assertThat(counter.unit()).isEqualTo("count");
        assertThat(counter.labels()).containsExactly("method", "path");
        assertThat(counter.enabled()).isTrue();
    }

    @Test
    @DisplayName("Should create gauge metric")
    void shouldCreateGaugeMetric() {
        MetricDefinition gauge = MetricDefinition.gauge(
                "cpu_usage", "CPU usage percentage", "percent");

        assertThat(gauge.name()).isEqualTo("cpu_usage");
        assertThat(gauge.type()).isEqualTo(MetricType.GAUGE);
        assertThat(gauge.unit()).isEqualTo("percent");
    }

    @Test
    @DisplayName("Should create timer metric")
    void shouldCreateTimerMetric() {
        MetricDefinition timer = MetricDefinition.timer(
                "request_duration", "Request duration", List.of("endpoint"));

        assertThat(timer.type()).isEqualTo(MetricType.TIMER);
        assertThat(timer.unit()).isEqualTo("seconds");
    }

    @Test
    @DisplayName("Should create histogram metric")
    void shouldCreateHistogramMetric() {
        MetricDefinition histogram = MetricDefinition.histogram(
                "response_size", "Response size", "bytes", List.of("service"));

        assertThat(histogram.type()).isEqualTo(MetricType.HISTOGRAM);
        assertThat(histogram.unit()).isEqualTo("bytes");
    }

    @Test
    @DisplayName("Should throw for null name")
    void shouldThrowForNullName() {
        assertThatThrownBy(() -> MetricDefinition.counter(null, "desc", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for blank name")
    void shouldThrowForBlankName() {
        assertThatThrownBy(() -> MetricDefinition.counter("  ", "desc", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should generate unique ID")
    void shouldGenerateUniqueId() {
        MetricDefinition m1 = MetricDefinition.counter("metric1", "d", List.of());
        MetricDefinition m2 = MetricDefinition.counter("metric2", "d", List.of());

        assertThat(m1.id()).isNotEqualTo(m2.id());
    }
}
