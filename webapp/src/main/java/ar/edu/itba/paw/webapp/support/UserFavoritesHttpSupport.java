package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.services.car.FavCarService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * HTTP binding for {@code /users/{id}/favorites} (link-only collection + membership checks).
 */
@Component
public final class UserFavoritesHttpSupport {

    private final UserService userService;
    private final FavCarService favCarService;

    public UserFavoritesHttpSupport(final UserService userService, final FavCarService favCarService) {
        this.userService = userService;
        this.favCarService = favCarService;
    }

    public Response list(final long userId, final PaginationParams paging, final UriInfo uriInfo) {
        requireUser(userId);
        final Page<CarCard> favorites =
                favCarService.findMyFavorites(userId, paging.getZeroBasedPage(), paging.getPageSize());
        if (favorites.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<LinksDto> links = favorites.getContent().stream()
                .map(card -> LinksDto.ofSelf(RestUriUtils.carUri(uriInfo, card.getCarId()).toString()))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<LinksDto>>(links) {})
                .header("X-Total-Count", favorites.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) favorites.getTotalItems());
        return builder.build();
    }

    public Response check(final long userId, final long carId) {
        requireUser(userId);
        if (!favCarService.isFavorited(carId, userId)) {
            throw new CarNotFoundException(carId);
        }
        return Response.noContent().build();
    }

    public Response add(final long userId, final long carId) {
        requireUser(userId);
        favCarService.addFavorite(carId, userId);
        return Response.noContent().build();
    }

    public Response remove(final long userId, final long carId) {
        requireUser(userId);
        favCarService.removeFavorite(carId, userId);
        return Response.noContent().build();
    }

    private void requireUser(final long userId) {
        userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
    }
}
