package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.UiPaging;

/**
 * Tiny helper used by every paged owner-hub handler to detect out-of-range page query params and
 * issue a redirect to the clamped value. Before this helper the same 8-line
 * {@code clampZeroBasedPage + UriComponentsBuilder.fromHttpRequest(...).replaceQueryParam(...)}
 * block was inlined in five handlers across {@code MyCarsController} and
 * {@code MyReservationsController}.
 *
 * This is intentionally a stateless utility (no Spring component): all paginated handlers can
 * just call {@code PageClampRedirect.ifOutOfRange(...)}.
 */
public final class PageClampRedirect {

    private PageClampRedirect() {
    }

    /**
     * Returns a redirect that replaces the {@code pageQueryParam} with its clamped value when the
     * requested page is outside {@code [0, lastPage]}; returns {@code Optional.empty()} otherwise.
     *
     * @param requestedPage   the raw {@code page} query parameter (after {@code Math.max(0, ...)})
     * @param resultPage      the page returned by the underlying service call (used to read total
     *                        items and configured page size)
     * @param pageQueryParam  query-param key to rewrite (e.g. {@code "page"}, {@code "riderPage"})
     * @param request         the current {@link HttpServletRequest} (so the redirect preserves
     *                        every other query parameter)
     */
    public static Optional<ModelAndView> ifOutOfRange(
            final int requestedPage,
            final Page<?> resultPage,
            final String pageQueryParam,
            final HttpServletRequest request) {
        final int safePage = UiPaging.clampZeroBasedPage(
                requestedPage, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safePage == requestedPage) {
            return Optional.empty();
        }
        final RedirectView redirectView = new RedirectView(
                UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                        .replaceQueryParam(pageQueryParam, safePage)
                        .build()
                        .toUriString());
        redirectView.setExposeModelAttributes(false);
        return Optional.of(new ModelAndView(redirectView));
    }
}
