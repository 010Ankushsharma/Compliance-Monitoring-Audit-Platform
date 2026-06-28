package com.company.compliance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing.
 *
 * <p>Note: this project uses {@link jakarta.persistence.PrePersist} /
 * {@link jakarta.persistence.PreUpdate} lifecycle callbacks directly on
 * entities rather than Spring's {@code @CreatedDate} / {@code @LastModifiedDate}
 * annotations, so this config is minimal. It is kept as a placeholder for
 * enabling JPA auditor-aware features if needed in the future.
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {
    // Spring Data JPA auditing enabled — entities handle timestamps via @PrePersist/@PreUpdate
}
