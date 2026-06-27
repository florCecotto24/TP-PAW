package ar.edu.itba.paw.webapp.controller.catalog;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.ModelDto;
import ar.edu.itba.paw.webapp.form.catalog.CatalogApprovalForm;

/** Catalog models ({@code /models}, {@code /models/{id}}). */
@Path("/models")
@Component
public final class ModelController {

    private final CarModelService carModelService;
    private final AdminService adminService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ModelController(final CarModelService carModelService, final AdminService adminService) {
        this.carModelService = carModelService;
        this.adminService = adminService;
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

    @GET
    @Path("/{id}")
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response getModel(@PathParam("id") final long id) {
        final CarModel model = carModelService.findById(id)
                .orElseThrow(() -> new CarModelNotFoundException(id));
        return Response.ok(ModelDto.from(model, uriInfo)).build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Consumes(VndMediaType.MODEL_V1_JSON)
    @Produces(VndMediaType.MODEL_V1_JSON)
    public Response approveModel(@PathParam("id") final long id, @Valid final CatalogApprovalForm patch) {
        carModelService.findById(id)
                .orElseThrow(() -> new CarModelNotFoundException(id));

        final Locale locale = LocaleContextHolder.getLocale();
        adminService.validateCatalogEntry(id, locale);

        final CarModel updated = carModelService.findById(id)
                .orElseThrow(() -> new CarModelNotFoundException(id));
        return Response.ok(ModelDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    public Response rejectModel(@PathParam("id") final long id) {
        final Locale locale = LocaleContextHolder.getLocale();
        adminService.rejectCatalogEntry(id, locale);
        return Response.noContent().build();
    }
}
