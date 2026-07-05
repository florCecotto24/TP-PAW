package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.services.car.CarPublishingService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.form.car.CarPatchForm;
import ar.edu.itba.paw.webapp.form.car.CarReplaceForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CarCreateRequestSupport;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.RestCarSortMapper;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarStatusList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;
import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;

/**
 * Cars resource ({@code /cars}, {@code /cars/{id}}).
 */
@Path("/cars")
@Component
public class CarController {

    private final CarService carService;
    private final CarPublishingService carPublishingService;
    private final AdminService adminService;
    private final FormValidationSupport formValidationSupport;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;
    private final CarCreateRequestSupport carCreateRequestSupport;
    private final AppPaginationProperties paginationProperties;
    private final ObjectMapper objectMapper;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public CarController(
            final CarService carService,
            final CarPublishingService carPublishingService,
            final AdminService adminService,
            final FormValidationSupport formValidationSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final CarCreateRequestSupport carCreateRequestSupport,
            final AppPaginationProperties paginationProperties,
            final ObjectMapper objectMapper,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.carPublishingService = carPublishingService;
        this.adminService = adminService;
        this.formValidationSupport = formValidationSupport;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
        this.carCreateRequestSupport = carCreateRequestSupport;
        this.paginationProperties = paginationProperties;
        this.objectMapper = objectMapper;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    // A14 (audit): documented decision — a single collection whose visibility is a query-param
    // filter, not a different operation per role (see openapi.yaml for the full per-branch
    // breakdown): scope=admin (admin-only, whole catalog) / ownerId (self-or-admin sees every
    // status, anyone else sees active-only and can't pass status) / neither (public browse,
    // active-only). Not a @PreAuthorize candidate: each branch's rule is chosen by which query
    // params the caller sent, not a precondition on path/query alone.
    @GET
    @Produces({VndMediaType.CAR_V1_JSON, VndMediaType.CAR_V1_XML})
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
            @QueryParam("rating") final List<String> rating,
            @QueryParam("neighborhoodId") final Long neighborhoodId,
            @QueryParam("from") final String from,
            @QueryParam("until") final String until,
            @QueryParam("flexible") @DefaultValue("false") final boolean flexible,
            @QueryParam("flexMonth") @ValidYearMonth final String flexMonth,
            @QueryParam("flexDays") final Integer flexDays,
            @QueryParam("status") @ValidCarStatusList final List<String> status,
            @QueryParam("scope") final String scope,
            @QueryParam("sort") final String sort) {
        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getDefaultPageSize();
        final int zeroBasedPage = safePage - 1;
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        final List<Car.Type> categories = toCarTypes(category);
        final List<Car.Transmission> transmissions = toTransmissions(transmission);
        final List<Car.Powertrain> powertrains = toPowertrains(powertrain);
        final List<String> ratingBands = rating == null ? List.of() : rating;

        if ("admin".equalsIgnoreCase(scope)) {
            carResourceAccess.requireAdmin();
            final Page<Car> adminPage = adminService.listCars(zeroBasedPage, pageSize);
            return pagedCarsFromEntities(adminPage, safePage, pageSize);
        }

        if (ownerId != null) {
            final boolean selfOrAdmin = viewer != null
                    && (viewer.getUserId() == ownerId || carResourceAccess.isAdmin());
            if (status != null && !status.isEmpty() && !selfOrAdmin) {
                throw new javax.ws.rs.ForbiddenException();
            }
            final List<Car.Status> statuses =
                    carService.resolveOwnerListingStatuses(toCarStatuses(status), selfOrAdmin);
            final String internalSort = RestCarSortMapper.toInternalSort(sort);
            final var criteria = carService.buildOwnerCarSearchCriteria(
                    ownerId,
                    categories.isEmpty() ? null : categories,
                    transmissions.isEmpty() ? null : transmissions,
                    powertrains.isEmpty() ? null : powertrains,
                    priceMin,
                    priceMax,
                    statuses,
                    ratingBands.isEmpty() ? null : ratingBands,
                    query,
                    zeroBasedPage,
                    pageSize,
                    internalSort);
            final Page<CarCard> ownerPage = carService.getOwnerCarCards(criteria);
            return pagedCarsFromCards(ownerPage, safePage, pageSize, false);
        }

        if (RestCarSortMapper.isBrowseShortcut(sort) && isPublicBrowseShortcut(query, category, transmission,
                powertrain, priceMin, priceMax, rating, neighborhoodId, from, until, status, flexible)) {
            final Page<CarCard> shortcutPage = "price_asc".equalsIgnoreCase(sort.trim())
                    ? carService.getCheapestCarCards(zeroBasedPage, pageSize)
                    : carService.getMostRecentCarCards(zeroBasedPage, pageSize);
            return pagedCarsFromCards(shortcutPage, safePage, pageSize, true);
        }

        final CarSearchRequest searchRequest = CarSearchRequest.builder()
                .query(query)
                .categories(categories.isEmpty() ? null : categories)
                .transmissions(transmissions.isEmpty() ? null : transmissions)
                .powertrains(powertrains.isEmpty() ? null : powertrains)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .ratingBands(ratingBands.isEmpty() ? null : ratingBands)
                .from(from)
                .until(until)
                .page(zeroBasedPage)
                .uiPageSize(pageSize)
                .sort(RestCarSortMapper.toInternalSort(sort))
                .neighborhoodIds(neighborhoodId == null ? Collections.emptyList() : List.of(neighborhoodId))
                .flexible(flexible)
                .flexMonth(flexMonth)
                .flexDays(flexDays)
                .build();
        final Page<CarCard> searchPage = carService.searchCarCards(carService.buildSearchCriteria(searchRequest));
        return pagedCarsFromCards(searchPage, safePage, pageSize, true);
    }

    @POST
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_V1_JSON)
    public Response publishCarJson(final CarCreateForm form) throws IOException {
        return publishCar(form, List.of(), null, null, null);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.CAR_V1_JSON)
    public Response publishCarMultipart(
            @FormDataParam("car") final InputStream carPart,
            @FormDataParam("pictures") final List<FormDataBodyPart> pictureParts,
            @FormDataParam("insurance") final FormDataBodyPart insurancePart) throws IOException {
        final byte[] insuranceBytes;
        final String insuranceName;
        final String insuranceType;
        if (insurancePart != null && insurancePart.getEntityAs(InputStream.class) != null) {
            insuranceBytes = insurancePart.getEntityAs(byte[].class);
            insuranceName = insurancePart.getContentDisposition() != null
                    ? insurancePart.getContentDisposition().getFileName()
                    : "insurance";
            insuranceType = insurancePart.getMediaType() != null
                    ? insurancePart.getMediaType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM;
        } else {
            insuranceBytes = null;
            insuranceName = null;
            insuranceType = null;
        }
        return publishCar(
                readCarCreateForm(carPart),
                readGalleryUploads(pictureParts),
                insuranceName,
                insuranceType,
                insuranceBytes);
    }

    private Response publishCar(
            final CarCreateForm form,
            final List<GalleryMediaUpload> galleryUploads,
            final String insuranceName,
            final String insuranceType,
            final byte[] insuranceBytes) throws IOException {
        final long ownerId = currentUserResolver.requireUserId();

        formValidationSupport.validate(form, ValidationGroups.OnPublishCar.class);

        final PublishCarRequest request = carCreateRequestSupport.toPublishRequest(form);

        final PublishCarRequest fullRequest = PublishCarRequest.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .type(request.getType())
                .plate(request.getPlate())
                .year(request.getYear())
                .powertrain(request.getPowertrain())
                .transmission(request.getTransmission())
                .description(request.getDescription())
                .galleryUploads(galleryUploads)
                .insurance(insuranceName, insuranceType, insuranceBytes)
                .build();

        final Locale locale = LocaleContextHolder.getLocale();
        final PublishCarOutcome outcome = carPublishingService.publishCar(ownerId, fullRequest, locale);
        final Car car = outcome.getCar();
        if (form.getMinimumRentalDays() != null && form.getMinimumRentalDays() > 0) {
            carService.updateMinimumRentalDays(car.getId(), form.getMinimumRentalDays());
        }

        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars")
                .path(String.valueOf(car.getId()))
                .build();
        final Car refreshed = carService.getCarById(car.getId()).orElse(car);
        return Response.created(location).entity(CarDto.from(refreshed, uriInfo)).build();
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.CAR_V1_JSON, VndMediaType.CAR_V1_XML})
    public Response getCar(@PathParam("id") final long id) {
        final Car car = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        return Response.ok(CarDto.from(car, uriInfo)).build();
    }

    @GET
    @Path("/{id}/similar")
    @Produces(VndMediaType.CAR_V1_JSON)
    public Response similarCars(
            @PathParam("id") final long id,
            @QueryParam("limit") @DefaultValue("4") final int limit) {
        carService.getCarById(id).orElseThrow(() -> new CarNotFoundException(id));
        final int safeLimit = Math.max(1, Math.min(limit, 20));
        final List<CarCard> cards = carService.findSimilarCarCards(id, safeLimit, null);
        if (cards.isEmpty()) {
            return Response.noContent().build();
        }
        final List<CarDto> dtos = toConsumerBrowseCarDtos(cards);
        return Response.ok(new GenericEntity<List<CarDto>>(dtos) {})
                .header("X-Total-Count", dtos.size())
                .build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response replaceCar(@P("id") @PathParam("id") final long id, @Valid final CarReplaceForm form) {
        final Car car = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        carService.updateDescription(car.getOwnerId(), id, form.getDescription());
        carService.updateMinimumRentalDays(id, form.getMinimumRentalDays());
        final Car updated = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        return Response.ok(CarDto.from(updated, uriInfo)).build();
    }

    // Not a @PreAuthorize candidate: "status" transitions apply owner- or admin-only rules
    // depending on the *target* status (a state-machine decision, see applyStatusPatch below),
    // and "description"/"minimumRentalDays" apply an owner-or-admin rule — both depend on which
    // fields the caller sent, so they stay imperative (this overlaps with A12's broader point
    // about status-transition business rules living in this controller).
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_V1_JSON)
    public Response patchCar(@PathParam("id") final long id, @Valid final CarPatchForm patch) {
        final Car car = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();

        if (patch.getStatus() != null) {
            applyStatusPatch(car, patch.getStatus(), viewer);
        }
        if (patch.getDescription() != null || patch.getMinimumRentalDays() != null) {
            carResourceAccess.requireOwnerOrAdmin(car, viewer);
            if (patch.getDescription() != null) {
                carService.updateDescription(car.getOwnerId(), id, patch.getDescription());
            }
            if (patch.getMinimumRentalDays() != null) {
                carService.updateMinimumRentalDays(id, patch.getMinimumRentalDays());
            }
        }

        final Car updated = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        return Response.ok(CarDto.from(updated, uriInfo)).build();
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deactivateCar(@P("id") @PathParam("id") final long id) {
        final Car car = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        if (!carService.deactivateCar(car.getOwnerId(), id)) {
            throw new CarNotFoundException(id);
        }
        return Response.noContent().build();
    }

    private void applyStatusPatch(final Car car, final String rawStatus, final RydenUserDetails viewer) {
        final Car.Status target = CarRestEnums.parseStatus(rawStatus);
        switch (target) {
            case ACTIVE -> {
                if (car.getStatus() == Car.Status.ADMIN_PAUSED) {
                    carResourceAccess.requireAdmin();
                    adminService.adminResumeCar(car.getId(), viewer.getUserId());
                } else {
                    carResourceAccess.requireOwner(car, viewer);
                    if (car.getStatus() != Car.Status.PAUSED) {
                        throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
                    }
                    carService.toggleCarStatus(car.getOwnerId(), car.getId());
                }
            }
            case PAUSED -> {
                carResourceAccess.requireOwner(car, viewer);
                if (car.getStatus() != Car.Status.ACTIVE) {
                    throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
                }
                carService.toggleCarStatus(car.getOwnerId(), car.getId());
            }
            case ADMIN_PAUSED -> {
                carResourceAccess.requireAdmin();
                adminService.adminPauseCar(
                        car.getId(), viewer.getUserId(), LocaleContextHolder.getLocale());
            }
            case DEACTIVATED -> {
                carResourceAccess.requireOwner(car, viewer);
                carService.deactivateCar(car.getOwnerId(), car.getId());
            }
            default -> throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
    }

    private Response pagedCarsFromCards(
            final Page<CarCard> page, final int safePage, final int pageSize, final boolean consumerBrowse) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarDto> dtos = consumerBrowse
                ? toConsumerBrowseCarDtos(page.getContent())
                : page.getContent().stream()
                        .map(card -> CarDto.fromCarCard(card, uriInfo))
                        .collect(Collectors.toList());
        return pagedOk(dtos, safePage, pageSize, (int) page.getTotalItems());
    }

    private List<CarDto> toConsumerBrowseCarDtos(final List<CarCard> cards) {
        final Map<Long, ConsumerCarCardMarketContext> marketContexts =
                carService.resolveConsumerPriceMarketContexts(cards);
        return CarDto.fromConsumerBrowseCarCards(cards, uriInfo, marketContexts);
    }

    private Response pagedCarsFromEntities(final Page<Car> page, final int safePage, final int pageSize) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarDto> dtos = page.getContent().stream()
                .map(car -> CarDto.from(car, uriInfo))
                .collect(Collectors.toList());
        return pagedOk(dtos, safePage, pageSize, (int) page.getTotalItems());
    }

    private Response pagedOk(
            final List<CarDto> dtos, final int safePage, final int pageSize, final int totalItems) {
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<CarDto>>(dtos) {})
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, totalItems);
        return builder.build();
    }

    private static boolean isPublicBrowseShortcut(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final Long neighborhoodId,
            final String from,
            final String until,
            final List<String> status,
            final boolean flexible) {
        return (query == null || query.isBlank())
                && (category == null || category.isEmpty())
                && (transmission == null || transmission.isEmpty())
                && (powertrain == null || powertrain.isEmpty())
                && priceMin == null
                && priceMax == null
                && (rating == null || rating.isEmpty())
                && neighborhoodId == null
                && (from == null || from.isBlank())
                && (until == null || until.isBlank())
                && (status == null || status.isEmpty())
                && !flexible;
    }

    private static List<Car.Type> toCarTypes(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseType);
    }

    private static List<Car.Transmission> toTransmissions(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseTransmission);
    }

    private static List<Car.Powertrain> toPowertrains(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parsePowertrain);
    }

    private static List<Car.Status> toCarStatuses(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseStatus);
    }

    private static <E> List<E> toDistinctCarEnums(final List<String> raw, final Function<String, E> parser) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        final LinkedHashSet<E> out = new LinkedHashSet<>();
        for (final String token : raw) {
            final E parsed = parser.apply(token);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return List.copyOf(out);
    }

    private CarCreateForm readCarCreateForm(final InputStream carPart) throws IOException {
        final byte[] carJson = binaryPayloadSupport.readValidatedBody(carPart);
        return objectMapper.readValue(carJson, CarCreateForm.class);
    }

    private List<GalleryMediaUpload> readGalleryUploads(final List<FormDataBodyPart> pictureParts)
            throws IOException {
        if (pictureParts == null || pictureParts.isEmpty()) {
            return List.of();
        }
        final List<GalleryMediaUpload> uploads = new ArrayList<>();
        for (final FormDataBodyPart part : pictureParts) {
            if (part == null) {
                continue;
            }
            final byte[] bytes = part.getEntityAs(byte[].class);
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            final String filename = part.getContentDisposition() != null
                    ? part.getContentDisposition().getFileName()
                    : "upload";
            final String contentType = part.getMediaType() != null
                    ? part.getMediaType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM;
            uploads.add(new GalleryMediaUpload(filename, contentType, bytes));
        }
        return uploads;
    }
}
