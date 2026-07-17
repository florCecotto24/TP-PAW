package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.support.CarInsuranceHttpSupport;

/**
 * Car insurance document ({@code /cars/{id}/insurance}). HTTP routing only.
 * Owner/admin gates are declarative ({@code @PreAuthorize} on {@code carResourceAccess}).
 */
@Path("/cars/{id}/insurance")
@Component
public class CarInsuranceController {

    private final CarInsuranceHttpSupport carInsuranceHttpSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public CarInsuranceController(final CarInsuranceHttpSupport carInsuranceHttpSupport) {
        this.carInsuranceHttpSupport = carInsuranceHttpSupport;
    }

    @GET
    @PreAuthorize("@carResourceAccess.isOwnerOrAdminById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response downloadInsurance(@P("id") @PathParam("id") final long carId) {
        return carInsuranceHttpSupport.download(carId);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadInsurance(@P("id") @PathParam("id") final long carId, final InputStream body)
            throws IOException {
        return carInsuranceHttpSupport.upload(carId, body, httpHeaders);
    }

    @DELETE
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteInsurance(@P("id") @PathParam("id") final long carId) {
        return carInsuranceHttpSupport.delete(carId);
    }
}
