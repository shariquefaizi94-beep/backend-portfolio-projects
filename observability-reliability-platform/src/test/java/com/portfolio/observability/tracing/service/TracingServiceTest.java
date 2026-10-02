package com.portfolio.observability.tracing.service;

import com.portfolio.observability.tracing.model.SpanKind;
import com.portfolio.observability.tracing.model.SpanStatus;
import com.portfolio.observability.tracing.model.TraceSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TracingService Tests")
class TracingServiceTest {

    private TracingService tracingService;

    @BeforeEach
    void setUp() {
        tracingService = new TracingService();
    }

    @Nested
    @DisplayName("Span Recording Tests")
    class SpanRecordingTests {

        @Test
        @DisplayName("Should record a span")
        void shouldRecordSpan() {
            TraceSpan span = TraceSpan.create(
                    "trace-123", "span-456", "GET /api/users", "api-gateway", SpanKind.SERVER);

            TraceSpan recorded = tracingService.recordSpan(span);

            assertThat(recorded).isNotNull();
            assertThat(recorded.traceId()).isEqualTo("trace-123");
            assertThat(recorded.spanId()).isEqualTo("span-456");
        }

        @Test
        @DisplayName("Should start a new span")
        void shouldStartNewSpan() {
            String traceId = UUID.randomUUID().toString();
            
            TraceSpan span = tracingService.startSpan(
                    traceId, "database-query", "user-service", SpanKind.CLIENT);

            assertThat(span).isNotNull();
            assertThat(span.traceId()).isEqualTo(traceId);
            assertThat(span.operationName()).isEqualTo("database-query");
            assertThat(span.startTime()).isNotNull();
        }

        @Test
        @DisplayName("Should start a child span")
        void shouldStartChildSpan() {
            String traceId = UUID.randomUUID().toString();
            TraceSpan parent = tracingService.startSpan(traceId, "parent-op", "service", SpanKind.SERVER);
            
            TraceSpan child = tracingService.startChildSpan(
                    traceId, parent.spanId(), "child-op", "service", SpanKind.INTERNAL);

            assertThat(child.parentSpanId()).isEqualTo(parent.spanId());
            assertThat(child.traceId()).isEqualTo(traceId);
            assertThat(child.isRoot()).isFalse();
        }

        @Test
        @DisplayName("Should end a span")
        void shouldEndSpan() {
            TraceSpan span = tracingService.startSpan(
                    "trace-1", "operation", "service", SpanKind.SERVER);
            
            TraceSpan ended = tracingService.endSpan(span.spanId());

            assertThat(ended.endTime()).isNotNull();
        }

        @Test
        @DisplayName("Should end a span with status")
        void shouldEndSpanWithStatus() {
            TraceSpan span = tracingService.startSpan(
                    "trace-1", "operation", "service", SpanKind.SERVER);
            
            TraceSpan ended = tracingService.endSpan(span.spanId(), SpanStatus.ERROR);

            assertThat(ended.status()).isEqualTo(SpanStatus.ERROR);
            assertThat(ended.isError()).isTrue();
        }

        @Test
        @DisplayName("Should throw when ending non-existent span")
        void shouldThrowWhenEndingNonExistentSpan() {
            assertThatThrownBy(() -> tracingService.endSpan("non-existent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should record multiple spans in batch")
        void shouldRecordMultipleSpansInBatch() {
            List<TraceSpan> spans = List.of(
                    TraceSpan.create("t1", "s1", "op1", "svc", SpanKind.SERVER),
                    TraceSpan.create("t1", "s2", "op2", "svc", SpanKind.CLIENT),
                    TraceSpan.create("t1", "s3", "op3", "svc", SpanKind.INTERNAL)
            );

            List<TraceSpan> recorded = tracingService.recordSpans(spans);

            assertThat(recorded).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Span Query Tests")
    class SpanQueryTests {

        private String traceId;

        @BeforeEach
        void setUp() {
            traceId = UUID.randomUUID().toString();
            tracingService.recordSpan(TraceSpan.create(traceId, "span-1", "op1", "service-a", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.child(traceId, "span-2", "span-1", "op2", "service-a", SpanKind.CLIENT));
            tracingService.recordSpan(TraceSpan.child(traceId, "span-3", "span-2", "op3", "service-b", SpanKind.SERVER));
        }

        @Test
        @DisplayName("Should get span by ID")
        void shouldGetSpanById() {
            var span = tracingService.getSpan("span-1");

            assertThat(span).isPresent();
            assertThat(span.get().operationName()).isEqualTo("op1");
        }

        @Test
        @DisplayName("Should get spans by trace ID")
        void shouldGetSpansByTraceId() {
            List<TraceSpan> spans = tracingService.getSpansByTraceId(traceId);

            assertThat(spans).hasSize(3);
        }

        @Test
        @DisplayName("Should get spans by service")
        void shouldGetSpansByService() {
            List<TraceSpan> serviceASpans = tracingService.getSpansByService("service-a");
            List<TraceSpan> serviceBSpans = tracingService.getSpansByService("service-b");

            assertThat(serviceASpans).hasSize(2);
            assertThat(serviceBSpans).hasSize(1);
        }

        @Test
        @DisplayName("Should get child spans")
        void shouldGetChildSpans() {
            List<TraceSpan> children = tracingService.getChildSpans("span-1");

            assertThat(children).hasSize(1);
            assertThat(children.get(0).spanId()).isEqualTo("span-2");
        }
    }

    @Nested
    @DisplayName("Trace Operations Tests")
    class TraceOperationsTests {

        @Test
        @DisplayName("Should get complete trace")
        void shouldGetCompleteTrace() {
            String traceId = UUID.randomUUID().toString();
            tracingService.recordSpan(TraceSpan.create(traceId, "root", "entry", "gateway", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.child(traceId, "child1", "root", "db-query", "api", SpanKind.CLIENT));
            tracingService.recordSpan(TraceSpan.child(traceId, "child2", "root", "cache-check", "api", SpanKind.CLIENT));

            TracingService.Trace trace = tracingService.getTrace(traceId);

            assertThat(trace).isNotNull();
            assertThat(trace.traceId()).isEqualTo(traceId);
            assertThat(trace.spanCount()).isEqualTo(3);
            assertThat(trace.services()).containsExactlyInAnyOrder("gateway", "api");
            assertThat(trace.rootSpan()).isNotNull();
            assertThat(trace.rootSpan().isRoot()).isTrue();
        }

        @Test
        @DisplayName("Should return null for non-existent trace")
        void shouldReturnNullForNonExistentTrace() {
            TracingService.Trace trace = tracingService.getTrace("non-existent");

            assertThat(trace).isNull();
        }

        @Test
        @DisplayName("Should find trace IDs in time range")
        void shouldFindTraceIdsInTimeRange() {
            tracingService.recordSpan(TraceSpan.create("trace-1", "s1", "op", "svc", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.create("trace-2", "s2", "op", "svc", SpanKind.SERVER));

            List<String> traceIds = tracingService.findTraceIds(
                    Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS), 
                    Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS), 100);

            assertThat(traceIds).hasSize(2);
        }

        @Test
        @DisplayName("Should delete trace")
        void shouldDeleteTrace() {
            String traceId = UUID.randomUUID().toString();
            tracingService.recordSpan(TraceSpan.create(traceId, "s1", "op", "svc", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.child(traceId, "s2", "s1", "op2", "svc", SpanKind.CLIENT));

            tracingService.deleteTrace(traceId);

            assertThat(tracingService.getSpansByTraceId(traceId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Slow and Error Span Tests")
    class SlowAndErrorSpanTests {

        @Test
        @DisplayName("Should find error spans")
        void shouldFindErrorSpans() {
            TraceSpan okSpan = tracingService.startSpan("t1", "ok-op", "svc", SpanKind.SERVER);
            TraceSpan errorSpan = tracingService.startSpan("t2", "error-op", "svc", SpanKind.SERVER);
            
            tracingService.endSpan(okSpan.spanId(), SpanStatus.OK);
            tracingService.endSpan(errorSpan.spanId(), SpanStatus.ERROR);

            List<TraceSpan> errors = tracingService.getErrorSpans();

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).operationName()).isEqualTo("error-op");
        }

        @Test
        @DisplayName("Should find error spans by service")
        void shouldFindErrorSpansByService() {
            TraceSpan span = tracingService.startSpan("t1", "error-op", "error-service", SpanKind.SERVER);
            tracingService.endSpan(span.spanId(), SpanStatus.ERROR);

            List<TraceSpan> errors = tracingService.getErrorSpansByService("error-service");

            assertThat(errors).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Service Discovery Tests")
    class ServiceDiscoveryTests {

        @BeforeEach
        void setUp() {
            tracingService.recordSpan(TraceSpan.create("t1", "s1", "op", "service-a", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.create("t2", "s2", "op", "service-b", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.create("t3", "s3", "op", "service-a", SpanKind.SERVER));
        }

        @Test
        @DisplayName("Should get distinct services")
        void shouldGetDistinctServices() {
            List<String> services = tracingService.getDistinctServices();

            assertThat(services).containsExactlyInAnyOrder("service-a", "service-b");
        }

        @Test
        @DisplayName("Should get span count by service")
        void shouldGetSpanCountByService() {
            Map<String, Long> counts = tracingService.getSpanCountByService();

            assertThat(counts).containsEntry("service-a", 2L);
            assertThat(counts).containsEntry("service-b", 1L);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should return tracing statistics")
        void shouldReturnTracingStatistics() {
            tracingService.recordSpan(TraceSpan.create("t1", "s1", "op", "svc", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.create("t2", "s2", "op", "svc", SpanKind.SERVER));

            var stats = tracingService.getStatistics();

            assertThat(stats.totalSpans()).isEqualTo(2);
            assertThat(stats.totalTraces()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return service-specific statistics")
        void shouldReturnServiceStatistics() {
            TraceSpan span = tracingService.startSpan("t1", "op", "test-service", SpanKind.SERVER);
            tracingService.endSpan(span.spanId(), SpanStatus.OK);

            var stats = tracingService.getServiceStatistics("test-service");

            assertThat(stats.serviceName()).isEqualTo("test-service");
            assertThat(stats.totalSpans()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Maintenance Tests")
    class MaintenanceTests {

        @Test
        @DisplayName("Should clear all data")
        void shouldClearAllData() {
            tracingService.recordSpan(TraceSpan.create("t1", "s1", "op", "svc", SpanKind.SERVER));
            tracingService.recordSpan(TraceSpan.create("t2", "s2", "op", "svc", SpanKind.SERVER));

            tracingService.clear();

            assertThat(tracingService.getStatistics().totalSpans()).isZero();
            assertThat(tracingService.getDistinctServices()).isEmpty();
        }
    }
}
