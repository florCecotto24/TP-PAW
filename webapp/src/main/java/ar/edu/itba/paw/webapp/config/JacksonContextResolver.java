package ar.edu.itba.paw.webapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/** Shares the Spring {@link ObjectMapper} with Jersey JSON providers. */
@Provider
@Component
public final class JacksonContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    @Autowired
    public JacksonContextResolver(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return objectMapper;
    }
}
