package ar.edu.itba.paw.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Shared Jackson setup for REST JSON payloads (ISO-8601 dates, not epoch numbers). */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    // Bean explícito (no @Component + component-scan): "ar.edu.itba.paw.webapp.config" no está entre
    // los basePackages de @ComponentScan en WebConfig, así que Jersey's SpringComponentProvider no
    // encontraba ningún bean Spring de este tipo y caía a instanciarlo por su cuenta vía HK2 (log GRAVE
    // "None or multiple beans found ... skipping the type" en cada arranque). Registrarlo acá lo deja
    // como bean real, resoluble sin ambigüedad.
    @Bean
    public JacksonContextResolver jacksonContextResolver(final ObjectMapper objectMapper) {
        return new JacksonContextResolver(objectMapper);
    }
}
