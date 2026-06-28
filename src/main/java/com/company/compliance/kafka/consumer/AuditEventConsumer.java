package com.company.compliance.kafka.consumer;

import com.company.compliance.event.AuditEvent;
import com.company.compliance.service.AuditLogService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that persists audit events to the database.
 *
 * <p>Concurrency is set to 3 (matching the topic's partition count) so all
 * partitions are consumed in parallel. Within each partition, ordering is
 * guaranteed — which is essential for hash-chain continuity per organisation.
 *
 * <p>Acknowledgement strategy: {@code MANUAL_IMMEDIATE} — offset is committed
 * only after successful DB persistence. On failure, the error handler retries
 * 3 times then routes to the DLT ({@code compliance.audit-events.DLT}).
 *
 * <p>File: {@code src/main/java/com/company/compliance/kafka/consumer/AuditEventConsumer.java}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;
    private final MeterRegistry   meterRegistry;

    @KafkaListener(
            topics       = "#{@appProperties.kafka.auditEvents}",
            groupId      = "compliance-audit-consumer",
            concurrency  = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AuditEvent> record, Acknowledgment ack) {
        AuditEvent event = record.value();

        log.debug("Consuming audit event: action={} org={} partition={} offset={}",
                event.getAction(),
                event.getOrganizationId(),
                record.partition(),
                record.offset());

        try {
            auditLogService.persistAuditLog(
                    event.getOrganizationId(),
                    event.getUserId(),
                    event.getUserEmail(),
                    event.getAction(),
                    event.getResourceType(),
                    event.getResourceId(),
                    event.getHttpMethod(),
                    event.getEndpoint(),
                    event.getIpAddress(),
                    event.getUserAgent(),
                    event.getRequestId(),
                    event.getOutcome(),
                    event.getStatusCode(),
                    event.getDurationMs(),
                    event.getDetails()
            );

            meterRegistry.counter("audit.events.consumed",
                    "action", event.getAction() != null ? event.getAction() : "UNKNOWN",
                    "outcome", event.getOutcome() != null ? event.getOutcome() : "UNKNOWN")
                    .increment();

            // Commit offset only after successful persistence
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Failed to persist audit event: action={} org={} error={}",
                    event.getAction(), event.getOrganizationId(), ex.getMessage(), ex);
            // Do NOT acknowledge — let the error handler retry, then route to DLT
            throw ex;
        }
    }

    /**
     * Dead-Letter Topic consumer — logs DLT messages for manual investigation.
     * Extend to: store in a fallback table, send ops alert, or trigger replay.
     */
    @KafkaListener(
            topics  = "#{@appProperties.kafka.auditEvents}.DLT",
            groupId = "compliance-audit-dlt-consumer"
    )
    public void consumeDlt(ConsumerRecord<String, AuditEvent> record, Acknowledgment ack) {
        AuditEvent event = record.value();
        log.error("[DLT] Audit event could not be processed after all retries: " +
                        "action={} org={} partition={} offset={}",
                event.getAction(), event.getOrganizationId(),
                record.partition(), record.offset());

        meterRegistry.counter("audit.events.dlt",
                "action", event.getAction() != null ? event.getAction() : "UNKNOWN")
                .increment();

        // Acknowledge DLT message to prevent infinite reprocessing
        ack.acknowledge();
    }
}
