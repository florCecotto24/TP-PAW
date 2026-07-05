package ar.edu.itba.paw.webapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Shares the Spring {@link ObjectMapper} with Jersey JSON providers.
 *
 * Registered as a Spring bean explicitly in {@link JacksonConfig} (not via
 * {@code @Component} + component-scan: {@code ar.edu.itba.paw.webapp.config} is not among
 * {@code WebConfig}'s {@code @ComponentScan} base packages) so that Jersey's
 * {@code SpringComponentProvider} resolves exactly one Spring-managed instance for this type.
 */
@Provider
public final class JacksonContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    public JacksonContextResolver(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return objectMapper;
    }
}
