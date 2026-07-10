package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.support.RydenExceptionHttpStatus;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Provider
@Component
public final class RydenExceptionMapper implements ExceptionMapper<RydenException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RydenExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public RydenExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final RydenException exception) {
        final Response.Status status = RydenExceptionHttpStatus.statusFor(exception);
        LOGGER.atDebug()
                .addArgument(exception.getMessageCode())
                .addArgument(status.getStatusCode())
                .log("RydenException key={} status={}");
        return RydenExceptionResponseSupport.toResponse(exception, status, localeMessages);
    }
}
