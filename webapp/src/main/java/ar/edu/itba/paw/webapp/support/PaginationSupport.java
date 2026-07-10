package ar.edu.itba.paw.webapp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;

/**
 * Resolves {@link PaginationParams} from query params and {@link AppPaginationProperties}.
 */
@Component
public final class PaginationSupport {

    private final AppPaginationProperties paginationProperties;

    @Autowired
    public PaginationSupport(final AppPaginationProperties paginationProperties) {
        this.paginationProperties = paginationProperties;
    }

    public PaginationParams forBrowseCars(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getUiPageSize(),
                paginationProperties.getMaxPageSize());
    }

    public PaginationParams forDefaultCollection(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize());
    }

    public PaginationParams forAvailabilities(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getManagePeriodsPageSize(),
                paginationProperties.getMaxPageSize());
    }

    public PaginationParams forCarReviews(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getCarPublicReviewsPageSize(),
                paginationProperties.getMaxPageSize());
    }

    public PaginationParams forMessages(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getAdminReservationChatPageSize(),
                paginationProperties.getMaxPageSize());
    }

    public PaginationParams forCarGallery(final int page, final Integer pageSizeParam) {
        return PaginationParams.resolve(
                page,
                pageSizeParam,
                paginationProperties.getCarGalleryPageSize(),
                paginationProperties.getMaxPageSize());
    }
}
