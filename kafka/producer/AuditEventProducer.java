package com.company.compliance.kafka.producer;

import com.company.compliance.config.AppProperties;
import com.company.compliance.event.AuditEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for audit events.
 *
 * <p>Partition key = {@code organizationId} — guarantees all audit events for
 * a given tenant land on the same partition, preserving ordered delivery for
 * hash-chain construction.
 *
 * <p>Publishes metrics for monitoring via Micrometer/Prometheus:
 * <ul>
 *   <li>{@code audit.events.published.total} — success counter</li>
 *   <li>{@code audit.events.failed.total}    — failure counter</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/kafka/producer/AuditEventProducer.java}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties                 appProperties;
    private final MeterRegistry                 meterRegistry;

    /**
     * Publishes an {@link AuditEvent} to the audit-events topic asynchronously.
     *
     * @param event the audit event to publish
     * @return a {@link CompletableFuture} that completes when the broker acknowledges the message
     */
    public CompletableFuture<SendResult<String, Object>> publish(AuditEvent event) {
        String topic = appProperties.getKafka().getAuditEvents();

        // Key by orgId so all events for the same org go to the same partition (ordered)
        String partitionKey = event.getOrganizationId() != null
                ? event.getOrganizationId().toString()
                : "system";

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, partitionKey, event);

        // Inject trace headers for distributed tracing
        injectMdcHeaders(record);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(record).toCompletableFuture();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                counter("audit.events.published", "action", event.getAction()).increment();
                log.debug("Audit event published: action={} org={} offset={}",
                        event.getAction(),
                        partitionKey,
                        result.getRecordMetadata().offset());
            } else {
                counter("audit.events.failed", "action", event.getAction()).increment();
                log.error("Failed to publish audit event: action={} org={} error={}",
                        event.getAction(), partitionKey, ex.getMessage());
            }
        });

        return future;
    }

    /**
     * Publishes synchronously — only used in tests or when guaranteed delivery
     * is required before returning a response.
     */
    public SendResult<String, Object> publishSync(AuditEvent event) {
        try {
            return publish(event).get();
        } catch (Exception e) {
            throw new RuntimeException("Synchronous audit event publish failed", e);
        }
    }

    private void injectMdcHeaders(ProducerRecord<String, Object> record) {
        String traceId = org.slf4j.MDC.get("traceId");
        String spanId  = org.slf4j.MDC.get("spanId");
        if (traceId != null) {
            record.headers().add("X-Trace-Id", traceId.getBytes());
        }
        if (spanId != null) {
            record.headers().add("X-Span-Id", spanId.getBytes());
        }
    }

    private Counter counter(String name, String tagKey, String tagValue) {
        return Counter.builder(name)
                .tag(tagKey, tagValue != null ? tagValue : "UNKNOWN")
                .register(meterRegistry);
    }
}
