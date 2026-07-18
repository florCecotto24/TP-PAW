package ar.edu.itba.paw.webapp.support;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.services.car.CarListingPolicyService;
import ar.edu.itba.paw.services.car.CarMarketInsightService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.dto.rest.CarSummaryDto;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * HTTP orchestration for {@code GET /cars} list branches (admin catalog, owner fleet, public browse)
 * and query-token parsing. Keeps {@code CarController} thin: resource methods delegate here.
 */
@Component
public final class CarListSupport {

    private final CarService carService;
    private final CarListingPolicyService carListingPolicyService;
    private final CarMarketInsightService carMarketInsightService;
    private final AdminService adminService;
    private final CarResourceAccess carResourceAccess;

    public CarListSupport(
            final CarService carService,
            final CarListingPolicyService carListingPolicyService,
            final CarMarketInsightService carMarketInsightService,
            final AdminService adminService,
            final CarResourceAccess carResourceAccess) {
        this.carService = carService;
        this.carListingPolicyService = carListingPolicyService;
        this.carMarketInsightService = carMarketInsightService;
        this.adminService = adminService;
        this.carResourceAccess = carResourceAccess;
    }

    public boolean isStatusAll(final List<String> status) {
        return status != null
                && status.size() == 1
                && "all".equalsIgnoreCase(status.get(0));
    }

    public Response listAdminCatalog(final PaginationParams paging, final UriInfo uriInfo) {
        carResourceAccess.requireAdmin();
        final Page<Car> adminPage = adminService.listCars(paging.getZeroBasedPage(), paging.getPageSize());
        return pagedCarsFromEntities(adminPage, paging, uriInfo);
    }

    public Response listOwnerCars(
            final PaginationParams paging,
            final long ownerId,
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final List<String> status,
            final String sort,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        final boolean selfOrAdmin = viewer != null
                && (viewer.getUserId() == ownerId || carResourceAccess.isAdmin());
        if (status != null && !status.isEmpty() && !selfOrAdmin) {
            throw new javax.ws.rs.ForbiddenException();
        }
        final List<Car.Type> categories = toCarTypes(category);
        final List<Car.Transmission> transmissions = toTransmissions(transmission);
        final List<Car.Powertrain> powertrains = toPowertrains(powertrain);
        final List<String> ratingBands = rating == null ? List.of() : rating;
        final List<Car.Status> statuses =
                carListingPolicyService.resolveOwnerListingStatuses(toCarStatuses(status), selfOrAdmin);
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
        return pagedCarSummariesFromCards(ownerPage, paging, !selfOrAdmin, uriInfo);
    }

    public Response listPublicBrowse(
            final PaginationParams paging,
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final String priceMarket,
            final List<String> rating,
            final Long neighborhoodId,
            final String from,
            final String until,
            final boolean flexible,
            final String flexMonth,
            final Integer flexDays,
            final List<String> status,
            final String sort,
            final UriInfo uriInfo) {
        final PriceMarketPosition priceMarketPosition = parsePriceMarketPosition(priceMarket);

        if (RestCarSortMapper.isBrowseShortcut(sort) && isPublicBrowseShortcut(query, category, transmission,
                powertrain, priceMin, priceMax, rating, neighborhoodId, from, until, status, flexible)
                && priceMarketPosition == null) {
            final Page<CarCard> shortcutPage = "price_asc".equalsIgnoreCase(sort.trim())
                    ? carService.getCheapestCarCards(paging.getZeroBasedPage(), paging.getPageSize())
                    : carService.getMostRecentCarCards(paging.getZeroBasedPage(), paging.getPageSize());
            return pagedCarSummariesFromCards(shortcutPage, paging, true, uriInfo);
        }

        final List<Car.Type> categories = toCarTypes(category);
        final List<Car.Transmission> transmissions = toTransmissions(transmission);
        final List<Car.Powertrain> powertrains = toPowertrains(powertrain);
        final List<String> ratingBands = rating == null ? List.of() : rating;
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
        return pagedCarSummariesFromCards(searchPage, paging, true, uriInfo);
    }

    private Response pagedCarSummariesFromCards(
            final Page<CarCard> page,
            final PaginationParams paging,
            final boolean consumerBrowse,
            final UriInfo uriInfo) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarSummaryDto> dtos = consumerBrowse
                ? toConsumerBrowseCarSummaries(page.getContent(), uriInfo)
                : page.getContent().stream()
                        .map(card -> CarSummaryDto.fromCarCard(card, uriInfo, null))
                        .collect(Collectors.toList());
        return pagedSummariesOk(dtos, paging, (int) page.getTotalItems(), uriInfo);
    }

    private List<CarSummaryDto> toConsumerBrowseCarSummaries(final List<CarCard> cards, final UriInfo uriInfo) {
        final Map<Long, ConsumerCarCardMarketContext> marketContexts =
                carMarketInsightService.resolveConsumerPriceMarketContexts(cards);
        return CarSummaryDto.fromConsumerBrowseCarCards(cards, uriInfo, marketContexts);
    }

    private Response pagedCarsFromEntities(
            final Page<Car> page, final PaginationParams paging, final UriInfo uriInfo) {
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarDto> dtos = page.getContent().stream()
                .map(car -> CarDto.from(car, uriInfo, true))
                .collect(Collectors.toList());
        return pagedFullCarsOk(dtos, paging, (int) page.getTotalItems(), uriInfo);
    }

    private static Response pagedSummariesOk(
            final List<CarSummaryDto> dtos,
            final PaginationParams paging,
            final int totalItems,
            final UriInfo uriInfo) {
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<CarSummaryDto>>(dtos) {})
                        .type(VndMediaType.CAR_SUMMARY_V1_JSON)
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
        return builder.build();
    }

    private static Response pagedFullCarsOk(
            final List<CarDto> dtos,
            final PaginationParams paging,
            final int totalItems,
            final UriInfo uriInfo) {
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

    public Response similarCars(
            final long carId,
            final int limit,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        carResourceAccess.requireViewableCar(carId, viewer);
        final int safeLimit = Math.max(1, Math.min(limit, 20));
        final List<CarCard> cards = carService.findSimilarCarCards(carId, safeLimit, null);
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

    static PriceMarketPosition parsePriceMarketPosition(final String priceMarket) {
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

    static List<Car.Type> toCarTypes(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseType);
    }

    static List<Car.Transmission> toTransmissions(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parseTransmission);
    }

    static List<Car.Powertrain> toPowertrains(final List<String> raw) {
        return toDistinctCarEnums(raw, CarRestEnums::parsePowertrain);
    }

    private List<Car.Status> toCarStatuses(final List<String> raw) {
        if (raw == null || raw.isEmpty() || isStatusAll(raw)) {
            return List.of();
        }
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
}
