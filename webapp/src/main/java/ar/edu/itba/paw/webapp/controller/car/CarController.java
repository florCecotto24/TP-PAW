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

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.services.car.CarPublishingService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.dto.rest.CarSummaryDto;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.form.car.CarPatchForm;
import ar.edu.itba.paw.webapp.form.car.CarReplaceForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CarCreateRequestSupport;
import ar.edu.itba.paw.webapp.support.CarRepresentationSupport;
import ar.edu.itba.paw.webapp.support.CarRepresentationVersions;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.ConditionalJsonResponses;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.RestCarSortMapper;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarStatusList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;
import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

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
    private final PaginationSupport paginationSupport;
    private final CarRepresentationSupport carRepresentationSupport;
    private final ObjectMapper objectMapper;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public CarController(
            final CarService carService,
            final CarPublishingService carPublishingService,
            final AdminService adminService,
            final FormValidationSupport formValidationSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final CarCreateRequestSupport carCreateRequestSupport,
            final PaginationSupport paginationSupport,
            final CarRepresentationSupport carRepresentationSupport,
            final ObjectMapper objectMapper,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.carPublishingService = carPublishingService;
        this.adminService = adminService;
        this.formValidationSupport = formValidationSupport;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
        this.carCreateRequestSupport = carCreateRequestSupport;
        this.paginationSupport = paginationSupport;
        this.carRepresentationSupport = carRepresentationSupport;
        this.objectMapper = objectMapper;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    // A14 (audit): documented decision — a single collection whose visibility is a query-param
    // filter, not a different operation per role (see openapi.yaml for the full per-branch
    // breakdown): admin catalog (Accept car.v1 + admin, whole catalog) / ownerId
    // (self-or-admin sees every status, anyone else sees active-only and can't pass status) /
    // neither (public browse, active-only). Not a @PreAuthorize candidate: each branch's rule
    // is chosen by which query params the caller sent, not a precondition on path/query alone.
    @GET
    @Produces({VndMediaType.CAR_SUMMARY_V1_JSON, VndMediaType.CAR_V1_JSON})
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
        final PaginationParams paging = paginationSupport.forBrowseCars(page, pageSizeParam);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        final List<Car.Type> categories = toCarTypes(category);
        final List<Car.Transmission> transmissions = toTransmissions(transmission);
        final List<Car.Powertrain> powertrains = toPowertrains(powertrain);
        final List<String> ratingBands = rating == null ? List.of() : rating;

        if (ownerId == null && carRepresentationSupport.acceptsFullCar(httpHeaders)) {
            carResourceAccess.requireAdmin();
            final Page<Car> adminPage = adminService.listCars(paging.getZeroBasedPage(), paging.getPageSize());
            return pagedCarsFromEntities(adminPage, paging);
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
                    paging.getZeroBasedPage(),
                    paging.getPageSize(),
                    internalSort);
            final Page<CarCard> ownerPage = carService.getOwnerCarCards(criteria);
            // Public profile / counterparty grids use ownerId without self-or-admin access and
            // should show competitive-price badges like browse/favorites. Owner "Mis autos"
            // (self) and admin fleet views keep plain summaries without market enrichment.
            return pagedCarSummariesFromCards(ownerPage, paging, !selfOrAdmin);
        }

        final PriceMarketPosition priceMarketPosition = parsePriceMarketPosition(priceMarket);

        if (RestCarSortMapper.isBrowseShortcut(sort) && isPublicBrowseShortcut(query, category, transmission,
                powertrain, priceMin, priceMax, rating, neighborhoodId, from, until, status, flexible)
                && priceMarketPosition == null) {
            final Page<CarCard> shortcutPage = "price_asc".equalsIgnoreCase(sort.trim())
                    ? carService.getCheapestCarCards(paging.getZeroBasedPage(), paging.getPageSize())
                    : carService.getMostRecentCarCards(paging.getZeroBasedPage(), paging.getPageSize());
            return pagedCarSummariesFromCards(shortcutPage, paging, true);
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
                .page(paging.getZeroBasedPage())
                .uiPageSize(paging.getPageSize())
                .sort(RestCarSortMapper.toInternalSort(sort))
                .neighborhoodIds(neighborhoodId == null ? Collections.emptyList() : List.of(neighborhoodId))
                .flexible(flexible)
                .flexMonth(flexMonth)
                .flexDays(flexDays)
                .priceMarketPosition(priceMarketPosition)
                .build();
        final Page<CarCard> searchPage = carService.searchCarCards(carService.buildSearchCriteria(searchRequest));
        return pagedCarSummariesFromCards(searchPage, paging, true);
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
                .minimumRentalDays(request.getMinimumRentalDays())
                .galleryUploads(galleryUploads)
                .insurance(insuranceName, insuranceType, insuranceBytes)
                .build();

        final Locale locale = LocaleContextHolder.getLocale();
        final PublishCarOutcome outcome = carPublishingService.publishCar(ownerId, fullRequest, locale);
        final Car car = outcome.getCar();

        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars")
                .path(String.valueOf(car.getId()))
                .build();
        final Car refreshed = carService.getCarById(car.getId()).orElse(car);
        return Response.created(location).entity(CarDto.from(refreshed, uriInfo)).build();
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.CAR_SUMMARY_V1_JSON, VndMediaType.CAR_V1_JSON})
    public Response getCar(@PathParam("id") final long id, @Context final Request request) {
        final Car car = carResourceAccess.requireViewableCar(
                id, currentUserResolver.currentPrincipalOrNull());
        if (carRepresentationSupport.acceptsCarSummary(httpHeaders)) {
            return ConditionalJsonResponses.okOrNotModified(
                    request,
                    CarRepresentationVersions.etagValue(car, CarRepresentationVersions.SUMMARY),
                    VndMediaType.CAR_SUMMARY_V1_JSON,
                    () -> CarSummaryDto.from(car, uriInfo));
        }
        return ConditionalJsonResponses.okOrNotModified(
                request,
                CarRepresentationVersions.etagValue(car, CarRepresentationVersions.DETAIL),
                VndMediaType.CAR_V1_JSON,
                () -> CarDto.from(car, uriInfo));
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
        carResourceAccess.requireViewableCar(id, currentUserResolver.currentPrincipalOrNull());
        final int safeLimit = Math.max(1, Math.min(limit, 20));
        final List<CarCard> cards = carService.findSimilarCarCards(id, safeLimit, null);
        if (cards.isEmpty()) {
            return Response.noContent().build();
        }
        final List<LinksDto> links = cards.stream()
                .map(card -> LinksDto.ofSelf(RestUriUtils.carUri(uriInfo, card.getCarId()).toString()))
                .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<LinksDto>>(links) {})
                .header("X-Total-Count", links.size())
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
    // depending on the *target* status; authorization is enforced in CarService#applyStatusTransition.
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.CAR_V1_JSON)
    @Produces(VndMediaType.CAR_V1_JSON)
    public Response patchCar(@PathParam("id") final long id, @Valid final CarPatchForm patch) {
        final Car car = carService.getCarById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();

        if (patch.getStatus() != null) {
            final Car.Status target = CarRestEnums.parseStatus(patch.getStatus());
            carService.applyStatusTransition(
                    car.getId(), target, viewer.getUserId(), LocaleContextHolder.getLocale());
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

    private Response pagedCarSummariesFromCards(
            final Page<CarCard> page, final PaginationParams paging, final boolean consumerBrowse) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarSummaryDto> dtos = consumerBrowse
                ? toConsumerBrowseCarSummaries(page.getContent())
                : page.getContent().stream()
                        .map(card -> CarSummaryDto.fromCarCard(card, uriInfo, null))
                        .collect(Collectors.toList());
        return pagedSummariesOk(dtos, paging, (int) page.getTotalItems());
    }

    private List<CarSummaryDto> toConsumerBrowseCarSummaries(final List<CarCard> cards) {
        final Map<Long, ConsumerCarCardMarketContext> marketContexts =
                carService.resolveConsumerPriceMarketContexts(cards);
        return CarSummaryDto.fromConsumerBrowseCarCards(cards, uriInfo, marketContexts);
    }

    private Response pagedCarsFromEntities(final Page<Car> page, final PaginationParams paging) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarDto> dtos = page.getContent().stream()
                .map(car -> CarDto.from(car, uriInfo))
                .collect(Collectors.toList());
        return pagedFullCarsOk(dtos, paging, (int) page.getTotalItems());
    }

    private Response pagedSummariesOk(
            final List<CarSummaryDto> dtos, final PaginationParams paging, final int totalItems) {
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<CarSummaryDto>>(dtos) {})
                        .type(VndMediaType.CAR_SUMMARY_V1_JSON)
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
        return builder.build();
    }

    private Response pagedFullCarsOk(
            final List<CarDto> dtos, final PaginationParams paging, final int totalItems) {
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<CarDto>>(dtos) {})
                        .type(VndMediaType.CAR_V1_JSON)
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
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

    /** REST {@code priceMarket} → market-band browse filter; unknown values ignored. */
    private static PriceMarketPosition parsePriceMarketPosition(final String priceMarket) {
        if (priceMarket == null || priceMarket.isBlank()) {
            return null;
        }
        return switch (priceMarket.trim().toLowerCase(Locale.ROOT)) {
            case "below_market" -> PriceMarketPosition.BELOW_MARKET;
            case "at_market" -> PriceMarketPosition.AT_MARKET;
            case "above_market" -> PriceMarketPosition.ABOVE_MARKET;
            default -> null;
        };
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
