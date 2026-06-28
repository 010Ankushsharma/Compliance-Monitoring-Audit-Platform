package com.company.compliance.kafka.consumer;

import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.event.ViolationEvent;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.ComplianceViolationRepository;
import com.company.compliance.service.AlertService;
import com.company.compliance.service.RiskScoringService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that reacts to new violation events.
 *
 * <p>On each event:
 * <ol>
 *   <li>Creates an Alert (with deduplication) via {@link AlertService}</li>
 *   <li>Triggers async risk score recalculation for the policy</li>
 * </ol>
 *
 * <p>File: {@code src/main/java/com/company/compliance/kafka/consumer/ViolationEventConsumer.java}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationEventConsumer {

    private final ComplianceViolationRepository violationRepository;
    private final AlertService                  alertService;
    private final RiskScoringService            riskScoringService;
    private final MeterRegistry                 meterRegistry;

    @KafkaListener(
            topics           = "#{@appProperties.kafka.violations}",
            groupId          = "compliance-violation-consumer",
            concurrency      = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ViolationEvent> record, Acknowledgment ack) {
        ViolationEvent event = record.value();

        log.info("Processing violation event: violationId={} severity={} policy={} new={}",
                event.getViolationId(), event.getSeverity(),
                event.getPolicyName(), event.isNew());

        try {
            // Load the violation entity so AlertService has the full context
            ComplianceViolation violation = violationRepository
                    .findByIdAndOrganizationId(
                            event.getViolationId(), event.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Violation", event.getViolationId()));

            // Create or deduplicate alert
            if (event.isNew()) {
                alertService.createFromViolation(violation);
            }

            // Always recalculate risk score on violation events
            riskScoringService.recalculateAsync(
                    event.getPolicyId(), event.getOrganizationId());

            meterRegistry.counter("violations.events.consumed",
                    "severity", event.getSeverity().getValue(),
                    "new",      String.valueOf(event.isNew()))
                    .increment();

            ack.acknowledge();

        } catch (ResourceNotFoundException e) {
            log.error("Violation {} not found — skipping. Possible race condition.", 
                    event.getViolationId());
            ack.acknowledge(); // Don't retry for missing entities — data is gone
        } catch (Exception ex) {
            log.error("Failed to process violation event {}: {}",
                    event.getViolationId(), ex.getMessage(), ex);
            throw ex; // Trigger retry → DLT after exhaustion
        }
    }

    @KafkaListener(
            topics  = "#{@appProperties.kafka.violations}.DLT",
            groupId = "compliance-violation-dlt-consumer"
    )
    public void consumeDlt(ConsumerRecord<String, ViolationEvent> record, Acknowledgment ack) {
        ViolationEvent event = record.value();
        log.error("[DLT] Violation event could not be processed: violationId={} severity={}",
                event.getViolationId(), event.getSeverity());
        meterRegistry.counter("violations.events.dlt",
                "severity", event.getSeverity() != null
                        ? event.getSeverity().getValue() : "UNKNOWN").increment();
        ack.acknowledge();
    }
}
