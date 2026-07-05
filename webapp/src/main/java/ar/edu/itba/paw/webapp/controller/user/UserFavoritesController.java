package ar.edu.itba.paw.webapp.controller.user;

import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.car.FavCarService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;

/**
 * Favorite cars ({@code /users/{id}/favorites}).
 *
 * <p>Authorization is fully declarative ({@code @PreAuthorize}, backed by the
 * {@code userResourceAccess}/{@code currentUserResolver} beans referenced by name) — no imperative
 * checks remain, so those beans aren't injected as fields here.
 */
@Path("/users/{id}/favorites")
@Component
public class UserFavoritesController {

    private final UserService userService;
    private final FavCarService favCarService;
    private final CarService carService;
    private final AppPaginationProperties paginationProperties;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserFavoritesController(
            final UserService userService,
            final FavCarService favCarService,
            final CarService carService,
            final AppPaginationProperties paginationProperties) {
        this.userService = userService;
        this.favCarService = favCarService;
        this.carService = carService;
        this.paginationProperties = paginationProperties;
    }

    @GET
    @Produces(VndMediaType.CAR_V1_JSON)
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response listFavorites(
            @P("id") @PathParam("id") final long id,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));

        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getDefaultPageSize();
        final Page<CarCard> favorites = favCarService.findMyFavorites(id, safePage - 1, pageSize);
        if (favorites.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<CarCard> cards = favorites.getContent();
        final Map<Long, ConsumerCarCardMarketContext> marketContexts =
                carService.resolveConsumerPriceMarketContexts(cards);
        final List<CarDto> dtos = CarDto.fromConsumerBrowseCarCards(cards, uriInfo, marketContexts);
        final Response.ResponseBuilder builder = Response.ok(dtos)
                .header("X-Total-Count", favorites.getTotalItems());
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) favorites.getTotalItems());
        return builder.build();
    }

    @PUT
    @Path("/{carId}")
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response addFavorite(
            @P("id") @PathParam("id") final long id, @PathParam("carId") final long carId) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!favCarService.isFavorited(carId, id)) {
            favCarService.toggleFavorite(carId, id);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{carId}")
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response removeFavorite(
            @P("id") @PathParam("id") final long id, @PathParam("carId") final long carId) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (favCarService.isFavorited(carId, id)) {
            favCarService.toggleFavorite(carId, id);
        }
        return Response.noContent().build();
    }
}
