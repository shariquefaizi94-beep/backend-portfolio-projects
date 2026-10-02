package com.portfolio.observability.tracing.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TraceSpan Tests")
class TraceSpanTest {

    @Test
    @DisplayName("Should create root span")
    void shouldCreateRootSpan() {
        TraceSpan span = TraceSpan.create(
                "trace-123", "span-456", "GET /api/users", "api-gateway", SpanKind.SERVER);

        assertThat(span.traceId()).isEqualTo("trace-123");
        assertThat(span.spanId()).isEqualTo("span-456");
        assertThat(span.parentSpanId()).isNull();
        assertThat(span.operationName()).isEqualTo("GET /api/users");
        assertThat(span.serviceName()).isEqualTo("api-gateway");
        assertThat(span.kind()).isEqualTo(SpanKind.SERVER);
        assertThat(span.status()).isEqualTo(SpanStatus.OK);
        assertThat(span.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should create child span")
    void shouldCreateChildSpan() {
        TraceSpan child = TraceSpan.child(
                "trace-123", "span-789", "span-456", "DB Query", "user-service", SpanKind.CLIENT);

        assertThat(child.parentSpanId()).isEqualTo("span-456");
        assertThat(child.isRoot()).isFalse();
    }

    @Test
    @DisplayName("Should end span")
    void shouldEndSpan() {
        TraceSpan span = TraceSpan.create("t", "s", "op", "svc", SpanKind.SERVER);

        TraceSpan ended = span.end();

        assertThat(ended.endTime()).isNotNull();
        assertThat(ended.endTime()).isAfterOrEqualTo(ended.startTime());
    }

    @Test
    @DisplayName("Should end span with status")
    void shouldEndSpanWithStatus() {
        TraceSpan span = TraceSpan.create("t", "s", "op", "svc", SpanKind.SERVER);

        TraceSpan ended = span.end(SpanStatus.ERROR);

        assertThat(ended.status()).isEqualTo(SpanStatus.ERROR);
        assertThat(ended.isError()).isTrue();
    }

    @Test
    @DisplayName("Should add tags")
    void shouldAddTags() {
        TraceSpan span = TraceSpan.create("t", "s", "op", "svc", SpanKind.SERVER);

        TraceSpan withTags = span.withTags(Map.of("http.method", "GET", "http.status_code", "200"));

        assertThat(withTags.tags()).containsEntry("http.method", "GET");
        assertThat(withTags.tags()).containsEntry("http.status_code", "200");
    }

    @Test
    @DisplayName("Should calculate duration")
    void shouldCalculateDuration() {
        TraceSpan span = TraceSpan.create("t", "s", "op", "svc", SpanKind.SERVER).end();

        assertThat(span.duration()).isNotNull();
        assertThat(span.duration().toNanos()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should identify error status")
    void shouldIdentifyErrorStatus() {
        TraceSpan okSpan = TraceSpan.create("t", "s1", "op", "svc", SpanKind.SERVER);
        TraceSpan errorSpan = TraceSpan.create("t", "s2", "op", "svc", SpanKind.SERVER).end(SpanStatus.ERROR);
        TraceSpan timeoutSpan = TraceSpan.create("t", "s3", "op", "svc", SpanKind.SERVER).end(SpanStatus.TIMEOUT);

        assertThat(okSpan.isError()).isFalse();
        assertThat(errorSpan.isError()).isTrue();
        assertThat(timeoutSpan.isError()).isTrue();
    }

    @Test
    @DisplayName("Should throw for null trace ID")
    void shouldThrowForNullTraceId() {
        assertThatThrownBy(() -> 
                TraceSpan.create(null, "s", "op", "svc", SpanKind.SERVER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null span ID")
    void shouldThrowForNullSpanId() {
        assertThatThrownBy(() -> 
                TraceSpan.create("t", null, "op", "svc", SpanKind.SERVER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null operation name")
    void shouldThrowForNullOperationName() {
        assertThatThrownBy(() -> 
                TraceSpan.create("t", "s", null, "svc", SpanKind.SERVER))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
