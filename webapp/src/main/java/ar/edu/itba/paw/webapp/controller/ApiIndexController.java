package ar.edu.itba.paw.webapp.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.ApiIndexDto;

/** API entrypoint ({@code GET /}, {@code openapi.yaml} {@code ApiIndex}). */
@Path("/")
@Component
public final class ApiIndexController {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(VndMediaType.API_V1_JSON)
    public Response index() {
        return Response.ok(ApiIndexDto.from(uriInfo)).build();
    }
}
