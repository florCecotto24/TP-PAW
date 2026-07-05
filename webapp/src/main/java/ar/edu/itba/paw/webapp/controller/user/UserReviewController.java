package ar.edu.itba.paw.webapp.controller.user;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.UserReviewDto;

/** Reviews received by a user ({@code /users/{id}/reviews}). */
@Path("/users/{id}/reviews")
@Component
public final class UserReviewController {

    private final UserService userService;
    private final ReviewService reviewService;
    private final AppPaginationProperties paginationProperties;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserReviewController(
            final UserService userService,
            final ReviewService reviewService,
            final AppPaginationProperties paginationProperties) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.paginationProperties = paginationProperties;
    }

    @GET
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response listReviews(
            @PathParam("id") final long id,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));

        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getDefaultPageSize();
        final Page<ReviewItemDto> reviews = reviewService.getReviewsForUser(id, safePage - 1, pageSize);
        if (reviews.getTotalItems() == 0L) {
            return Response.noContent().build();
        }

        final List<UserReviewDto> dtos = reviews.getContent().stream()
                .map(item -> UserReviewDto.from(item, uriInfo))
                .collect(Collectors.toList());

        final Response.ResponseBuilder builder = Response.ok(dtos)
                .header("X-Total-Count", reviews.getTotalItems());
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) reviews.getTotalItems());
        return builder.build();
    }
}
