package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * UI-layer page sizes bound from {@code app.pagination.*}. Lives in the webapp module because
 * pagination is a UI concern (clamping, page-param redirects, choosing how many cards to render).
 * The DAOs paginate with SQL {@code LIMIT}/{@code OFFSET} using these same sizes.
 *
 * Architectural rule: services do not read these sizes. Controllers inject this component and
 * pass the resolved {@code pageSize} (or {@code uiPageSize}) into the service/criteria builder.
 */
@Component
public final class AppPaginationProperties {

    private static final String UI_PAGE_SIZE = "app.pagination.ui-page-size";
    private static final String DEFAULT_PAGE_SIZE = "app.pagination.default-page-size";
    private static final String LISTING_PUBLIC_REVIEWS_PAGE_SIZE = "app.pagination.listing-public-reviews-page-size";
    private static final String ADMIN_RESERVATION_CHAT_PAGE_SIZE = "app.pagination.admin-reservation-chat-page-size";
    private static final String MANAGE_PERIODS_PAGE_SIZE = "app.pagination.manage-periods-page-size";

    private static final int FALLBACK_UI_PAGE_SIZE = 8;
    private static final int FALLBACK_LISTING_PUBLIC_REVIEWS_PAGE_SIZE = 6;
    private static final int FALLBACK_ADMIN_RESERVATION_CHAT_PAGE_SIZE = 50;
    private static final int FALLBACK_MANAGE_PERIODS_PAGE_SIZE = 4;

    private final int uiPageSize;
    private final int defaultPageSize;
    private final int carPublicReviewsPageSize;
    private final int adminReservationChatPageSize;
    private final int managePeriodsPageSize;

    @Autowired
    public AppPaginationProperties(final Environment environment) {
        this.uiPageSize = readPositiveInt(environment, UI_PAGE_SIZE, FALLBACK_UI_PAGE_SIZE);
        final Integer legacyDefault = environment.getProperty(DEFAULT_PAGE_SIZE, Integer.class);
        this.defaultPageSize = legacyDefault != null && legacyDefault > 0 ? legacyDefault : this.uiPageSize;
        this.carPublicReviewsPageSize = readPositiveInt(
                environment, LISTING_PUBLIC_REVIEWS_PAGE_SIZE, FALLBACK_LISTING_PUBLIC_REVIEWS_PAGE_SIZE);
        this.adminReservationChatPageSize = readPositiveInt(
                environment, ADMIN_RESERVATION_CHAT_PAGE_SIZE, FALLBACK_ADMIN_RESERVATION_CHAT_PAGE_SIZE);
        this.managePeriodsPageSize = readPositiveInt(
                environment, MANAGE_PERIODS_PAGE_SIZE, FALLBACK_MANAGE_PERIODS_PAGE_SIZE);
    }

    /** Used by search-style grids (home, /search) paired with the persistence DB fetch window. */
    public int getUiPageSize() {
        return uiPageSize;
    }

    /** Single SQL {@code LIMIT}/UI page for owner listings, reservations, favourites grids. */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /** Car-detail public reviews list. */
    public int getCarPublicReviewsPageSize() {
        return carPublicReviewsPageSize;
    }

    /** Admin reservation chat inspector page size. */
    public int getAdminReservationChatPageSize() {
        return adminReservationChatPageSize;
    }

    /** Manage-car-periods page size. */
    public int getManagePeriodsPageSize() {
        return managePeriodsPageSize;
    }

    private static int readPositiveInt(final Environment env, final String key, final int fallback) {
        final Integer v = env.getProperty(key, Integer.class);
        return v != null && v > 0 ? v : fallback;
    }
}
