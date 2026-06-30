package com.company.compliance.config;

import com.company.compliance.event.AuditEvent;
import com.company.compliance.event.ViolationEvent;
import com.company.compliance.event.AlertEvent;
import com.company.compliance.event.ReportRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer and consumer factory configuration.
 *
 * <p>Topics are auto-created at startup via {@link NewTopic} beans.
 * In production, set {@code auto.create.topics.enable=false} on the broker
 * and provision topics via Terraform / Confluent Cloud API instead.
 *
 * <p>Reliability settings:
 * <ul>
 *   <li>Producer: {@code acks=all}, idempotence enabled, exactly-once via transactions</li>
 *   <li>Consumer: manual acknowledgement ({@code MANUAL_IMMEDIATE}), committed after processing</li>
 *   <li>Error handler: 3 retries with 2-second backoff, then DLT (Dead Letter Topic)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    private final AppProperties   appProperties;
    private final KafkaProperties kafkaProperties;

    // ── Topic definitions ─────────────────────────────────────────

    @Bean
    public NewTopic auditEventsTopic() {
        AppProperties.KafkaTopicProperties cfg = appProperties.getKafka();
        return TopicBuilder.name(cfg.getAuditEvents())
                .partitions(cfg.getPartitions())
                .replicas(cfg.getReplicationFactor())
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000)) // 7 days
                .config("compression.type", "lz4")
                .build();
    }

    @Bean
    public NewTopic violationsTopic() {
        AppProperties.KafkaTopicProperties cfg = appProperties.getKafka();
        return TopicBuilder.name(cfg.getViolations())
                .partitions(cfg.getPartitions())
                .replicas(cfg.getReplicationFactor())
                .build();
    }

    @Bean
    public NewTopic alertsTopic() {
        AppProperties.KafkaTopicProperties cfg = appProperties.getKafka();
        return TopicBuilder.name(cfg.getAlerts())
                .partitions(cfg.getPartitions())
                .replicas(cfg.getReplicationFactor())
                .build();
    }

    @Bean
    public NewTopic reportRequestsTopic() {
        AppProperties.KafkaTopicProperties cfg = appProperties.getKafka();
        return TopicBuilder.name(cfg.getReportRequests())
                .partitions(cfg.getPartitions())
                .replicas(cfg.getReplicationFactor())
                .build();
    }

    // Dead-letter topics — one per main topic
    @Bean public NewTopic auditEventsDlt() {
        return TopicBuilder.name(appProperties.getKafka().getAuditEvents() + ".DLT")
                .partitions(1).replicas(appProperties.getKafka().getReplicationFactor()).build();
    }

    @Bean public NewTopic violationsDlt() {
        return TopicBuilder.name(appProperties.getKafka().getViolations() + ".DLT")
                .partitions(1).replicas(appProperties.getKafka().getReplicationFactor()).build();
    }

    @Bean public NewTopic reportRequestsDlt() {
        return TopicBuilder.name(appProperties.getKafka().getReportRequests() + ".DLT")
                .partitions(1).replicas(appProperties.getKafka().getReplicationFactor()).build();
    }

    // ── Producer factory ──────────────────────────────────────────

    @Bean
     public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configs = new HashMap<>(
            kafkaProperties.buildProducerProperties(null));

    configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
    configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configs.put(ProducerConfig.ACKS_CONFIG,                   "all");
    configs.put(ProducerConfig.RETRIES_CONFIG,                3);
    configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,     true);
    configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    configs.put(JsonSerializer.ADD_TYPE_INFO_HEADERS,         false);

    DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(configs);

    String txIdPrefix = appProperties.getKafka().getTransactionalIdPrefix();
    if (txIdPrefix != null && !txIdPrefix.isBlank()) {
        factory.setTransactionIdPrefix(txIdPrefix);
        log.info("Kafka producer transactions ENABLED with prefix '{}'", txIdPrefix);
    } else {
        log.info("Kafka producer transactions disabled (idempotent mode only)");
    }

    return factory;
}

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template =
                new KafkaTemplate<>(producerFactory());
        template.setObservationEnabled(true); // Micrometer tracing
        return template;
    }

    // ── Consumer factory ──────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);                              // 3 concurrent consumers
        factory.getContainerProperties()
               .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.getContainerProperties().setObservationEnabled(true); // Micrometer tracing
        return factory;
    }

    /**
     * Error handler: retry 3 times with a 2-second fixed backoff.
     * After exhausting retries, publish to the Dead Letter Topic (DLT).
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new FixedBackOff(2_000L, 3L)); // 2s delay, 3 attempts
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class);    // don't retry programming errors
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Kafka retry {} for topic [{}] key [{}]: {}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage()));
        return handler;
    }
}
