package com.company.compliance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global Jackson {@link ObjectMapper} configuration.
 *
 * <p>Key settings:
 * <ul>
 *   <li>Java 8 date/time types serialised as ISO-8601 strings (not timestamps)</li>
 *   <li>Unknown JSON properties silently ignored (forward-compatible DTOs)</li>
 *   <li>Null fields omitted from responses (configured via {@code @JsonInclude})</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
