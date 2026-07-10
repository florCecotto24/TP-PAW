package ar.edu.itba.paw.webapp.controller.catalog;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;

/** Admin pending-model collection only ({@code GET /models?validated=false}). */
@Path("/models")
@Component
public final class ModelController {

    private final CarModelService carModelService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ModelController(final CarModelService carModelService) {
        this.carModelService = carModelService;
    }

    @GET
    @RolesAllowed("ROLE_ADMIN")
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response listPendingModels(@QueryParam("validated") final Boolean validated) {
        if (validated == null || Boolean.TRUE.equals(validated)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final List<ModelDto> dtos = carModelService.findPendingOrdered().stream()
                .map(model -> ModelDto.from(model, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(new GenericEntity<List<ModelDto>>(dtos) {}).build();
    }
}
