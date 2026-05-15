package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.core.env.Environment;

import ar.edu.itba.paw.services.policy.ReservationChatPolicy;

/** Bound view of {@code app.reservation.chat.*}. */
public record AppReservationChatProperties(int historyPageSize, int graceDaysAfterFinished) {

    private static final String HISTORY_PAGE_SIZE = "app.reservation.chat.history-page-size";
    private static final String GRACE_DAYS_AFTER_FINISHED = "app.reservation.chat.grace-days-after-finished";

    public static AppReservationChatProperties fromEnvironment(final Environment environment) {
        return new AppReservationChatProperties(
                environment.getProperty(HISTORY_PAGE_SIZE, Integer.class, 50),
                environment.getProperty(GRACE_DAYS_AFTER_FINISHED, Integer.class, 7));
    }

    public ReservationChatPolicy toReservationChatPolicy() {
        return ReservationChatPolicy.fromValidatedConfiguration(graceDaysAfterFinished, historyPageSize);
    }
}
