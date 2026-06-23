package com.company.compliance.kafka.producer;

import com.company.compliance.config.AppProperties;
import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.event.ViolationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for violation detection events.
 *
 * <p>Partition key = {@code organizationId:policyId} — events for the same
 * policy are ordered, which matters for deduplication in the alert consumer.
 *
 * <p>File: {@code src/main/java/com/company/compliance/kafka/producer/ViolationEventProducer.java}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties                 appProperties;

    public CompletableFuture<?> publish(ComplianceViolation violation, boolean isNew) {
        ViolationEvent event = ViolationEvent.builder()
                .violationId(violation.getId())
                .organizationId(violation.getOrganization().getId())
                .policyId(violation.getPolicy().getId())
                .policyName(violation.getPolicy().getName())
                .framework(violation.getPolicy().getFramework().getValue())
                .policyRuleId(violation.getPolicyRule() != null
                        ? violation.getPolicyRule().getId() : null)
                .userId(violation.getUser() != null ? violation.getUser().getId() : null)
                .userEmail(violation.getUser() != null ? violation.getUser().getEmail() : null)
                .severity(violation.getSeverity())
                .title(violation.getTitle())
                .description(violation.getDescription())
                .evidence(violation.getEvidence())
                .detectedAt(violation.getDetectedAt())
                .isNew(isNew)
                .build();

        String partitionKey = violation.getOrganization().getId()
                + ":" + violation.getPolicy().getId();

        log.debug("Publishing violation event: violationId={} severity={} new={}",
                violation.getId(), violation.getSeverity(), isNew);

        return kafkaTemplate
                .send(appProperties.getKafka().getViolations(), partitionKey, event)
                .toCompletableFuture();
    }
}
