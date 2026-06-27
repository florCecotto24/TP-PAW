package ar.edu.itba.paw.webapp.controller.car;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.ReviewDto;

/** Public car reviews ({@code /cars/{id}/reviews}). */
@Path("/cars/{id}/reviews")
@Component
public final class CarReviewController {

    private final CarService carService;
    private final ReviewService reviewService;
    private final AppPaginationProperties paginationProperties;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public CarReviewController(
            final CarService carService,
            final ReviewService reviewService,
            final AppPaginationProperties paginationProperties) {
        this.carService = carService;
        this.reviewService = reviewService;
        this.paginationProperties = paginationProperties;
    }

    @GET
    @Produces(VndMediaType.REVIEW_V1_JSON)
    public Response listReviews(
            @PathParam("id") final long carId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getCarPublicReviewsPageSize();
        final Page<Review> reviews = reviewService.getCarPublicReviewEntities(
                carId, safePage - 1, pageSize);
        if (reviews.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<ReviewDto> dtos = reviews.getContent().stream()
                .map(review -> ReviewDto.from(review, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<ReviewDto>>(dtos) {})
                        .header("X-Total-Count", reviews.getTotalItems());
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) reviews.getTotalItems());
        return builder.build();
    }
}
