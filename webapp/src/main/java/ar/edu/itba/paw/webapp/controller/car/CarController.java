package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.form.car.CarPatchForm;
import ar.edu.itba.paw.webapp.support.CarItemSupport;
import ar.edu.itba.paw.webapp.support.CarListSupport;
import ar.edu.itba.paw.webapp.support.CarPatchSupport;
import ar.edu.itba.paw.webapp.support.CarPublishSupport;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarStatusList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;
import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;

/**
 * Cars resource ({@code /cars}, {@code /cars/{id}}).
 * HTTP routing only; list/publish/patch/item binding lives in {@code *Support} helpers.
 */
@Path("/cars")
@Component
public class CarController {

    private final CurrentUserResolver currentUserResolver;
    private final CarListSupport carListSupport;
    private final CarPublishSupport carPublishSupport;
    private final CarPatchSupport carPatchSupport;
    private final CarItemSupport carItemSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public CarController(
            final CurrentUserResolver currentUserResolver,
            final CarListSupport carListSupport,
            final CarPublishSupport carPublishSupport,
            final CarPatchSupport carPatchSupport,
            final CarItemSupport carItemSupport) {
        this.currentUserResolver = currentUserResolver;
        this.carListSupport = carListSupport;
        this.carPublishSupport = carPublishSupport;
        this.carPatchSupport = carPatchSupport;
        this.carItemSupport = carItemSupport;
    }

    // A14 (audit): documented decision — a single collection whose visibility is a query-param
    // filter, not a different operation per role (see openapi.yaml for the full per-branch
    // breakdown): admin catalog (Accept car.v1 + admin, whole catalog) / ownerId
    // (self-or-admin sees every status, anyone else sees active-only and can't pass status) /
    // neither (public browse, active-only). Not a @PreAuthorize candidate: each branch's rule
    // is chosen by which query params the caller sent, not a precondition on path/query alone.
    @GET
    @Produces({VndMediaType.CAR_SUMMARY_V1_JSON, VndMediaType.CAR_V1_JSON, VndMediaType.CAR_PRIVATE_V1_JSON})
    public Response listCars(
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam,
            @QueryParam("q") final String query,
            @QueryParam("ownerId") final Long ownerId,
            @QueryParam("category") @ValidCarTypeList final List<String> category,
            @QueryParam("transmission") @ValidCarTransmissionList final List<String> transmission,
            @QueryParam("powertrain") @ValidCarPowertrainList final List<String> powertrain,
            @QueryParam("priceMin") final BigDecimal priceMin,
            @QueryParam("priceMax") final BigDecimal priceMax,
            @QueryParam("priceMarket") final String priceMarket,
            @QueryParam("rating") final List<String> rating,
            @QueryParam("neighborhoodId") final Long neighborhoodId,
            @QueryParam("from") final String from,
            @QueryParam("until") final String until,
            @QueryParam("flexible") @DefaultValue("false") final boolean flexible,
            @QueryParam("flexMonth") @ValidYearMonth final String flexMonth,
            @QueryParam("flexDays") final Integer flexDays,
            @QueryParam("status") @ValidCarStatusList final List<String> status,
            @QueryParam("sort") final String sort) {
        return carListSupport.listCars(
                page,
                pageSizeParam,
                query,
                ownerId,
                category,
                transmission,
                powertrain,
                priceMin,
                priceMax,
                priceMarket,
                rating,
                neighborhoodId,
                from,
                until,
                flexible,
                flexMonth,
                flexDays,
                status,
                sort,
                currentUserResolver.currentPrincipalOrNull(),
                uriInfo);
    }

    @POST
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_PRIVATE_V1_JSON)
    public Response publishCarJson(final CarCreateForm form) throws IOException {
        return carPublishSupport.publishJson(
                currentUserResolver.requireUserId(),
                form,
                LocaleContextHolder.getLocale(),
                uriInfo);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.CAR_PRIVATE_V1_JSON)
    public Response publishCarMultipart(
            @FormDataParam("car") final InputStream carPart,
            @FormDataParam("pictures") final List<FormDataBodyPart> pictureParts,
            @FormDataParam("insurance") final FormDataBodyPart insurancePart) throws IOException {
        return carPublishSupport.publishMultipart(
                currentUserResolver.requireUserId(),
                carPart,
                pictureParts,
                insurancePart,
                LocaleContextHolder.getLocale(),
                uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.CAR_SUMMARY_V1_JSON, VndMediaType.CAR_V1_JSON, VndMediaType.CAR_PRIVATE_V1_JSON})
    public Response getCar(@PathParam("id") final long id, @Context final Request request) {
        return carItemSupport.get(
                id, currentUserResolver.currentPrincipalOrNull(), httpHeaders, request, uriInfo);
    }

    /**
     * Related cars as link-only collection ({@code car.similar.v1+json}). Clients follow each
     * {@code self} for the teaser — intentional HTTP N+1 to keep one canonical URI per car.
     */
    @GET
    @Path("/{id}/similar")
    @Produces(VndMediaType.CAR_SIMILAR_V1_JSON)
    public Response similarCars(
            @PathParam("id") final long id,
            @QueryParam("limit") @DefaultValue("4") final int limit) {
        return carListSupport.similarCars(id, limit, currentUserResolver.currentPrincipalOrNull(), uriInfo);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_PRIVATE_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerOrAdminById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response patchCar(
            @P("id") @PathParam("id") final long id,
            @Valid final CarPatchForm patch) {
        return carPatchSupport.apply(
                id,
                patch,
                currentUserResolver.requirePrincipal(),
                LocaleContextHolder.getLocale(),
                uriInfo);
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deactivateCar(@P("id") @PathParam("id") final long id) {
        return carItemSupport.deactivate(id);
    }
}
