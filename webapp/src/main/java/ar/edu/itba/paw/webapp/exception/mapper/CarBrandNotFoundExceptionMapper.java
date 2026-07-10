package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Provider
@Component
public final class CarBrandNotFoundExceptionMapper extends TypedRydenExceptionMapper<CarBrandNotFoundException> {

    @Autowired
    public CarBrandNotFoundExceptionMapper(final LocaleMessages localeMessages) {
        super(localeMessages, Response.Status.NOT_FOUND);
    }
}
