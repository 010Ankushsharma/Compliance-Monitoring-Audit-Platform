package com.company.compliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Compliance Monitoring & Audit Platform.
 *
 * <p>File: {@code src/main/java/com/company/compliance/ComplianceAuditPlatformApplication.java}
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class ComplianceAuditPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplianceAuditPlatformApplication.class, args);
    }
}
