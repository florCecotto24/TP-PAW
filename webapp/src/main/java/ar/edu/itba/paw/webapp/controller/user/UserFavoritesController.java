package ar.edu.itba.paw.webapp.controller.user;

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

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.UserFavoritesHttpSupport;

/**
 * Favorite cars ({@code /users/{id}/favorites}). HTTP routing only.
 * Authorization is declarative ({@code @PreAuthorize} on self).
 */
@Path("/users/{id}/favorites")
@Component
public class UserFavoritesController {

    private final UserFavoritesHttpSupport userFavoritesHttpSupport;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserFavoritesController(
            final UserFavoritesHttpSupport userFavoritesHttpSupport,
            final PaginationSupport paginationSupport) {
        this.userFavoritesHttpSupport = userFavoritesHttpSupport;
        this.paginationSupport = paginationSupport;
    }

    /**
     * Link-only favorite cars ({@code user.favorites.v1+json}). Clients follow each {@code self}
     * with {@code car.summary} — intentional HTTP N+1 (see {@code AGENTS.md}).
     */
    @GET
    @Produces(VndMediaType.USER_FAVORITES_V1_JSON)
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response listFavorites(
            @P("id") @PathParam("id") final long id,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        return userFavoritesHttpSupport.list(
                id, paginationSupport.forDefaultCollection(page, pageSizeParam), uriInfo);
    }

    @GET
    @Path("/{carId}")
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response checkFavorite(
            @P("id") @PathParam("id") final long id, @PathParam("carId") final long carId) {
        return userFavoritesHttpSupport.check(id, carId);
    }

    @PUT
    @Path("/{carId}")
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response addFavorite(
            @P("id") @PathParam("id") final long id, @PathParam("carId") final long carId) {
        return userFavoritesHttpSupport.add(id, carId);
    }

    @DELETE
    @Path("/{carId}")
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response removeFavorite(
            @P("id") @PathParam("id") final long id, @PathParam("carId") final long carId) {
        return userFavoritesHttpSupport.remove(id, carId);
    }
}
