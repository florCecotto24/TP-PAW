package ar.edu.itba.paw.webapp.form;

import java.time.LocalTime;

/**
 * Shared shape for listing publish/edit forms validated by {@code @CheckOutAfterCheckIn}.
 */
public interface ListingTimeWindow {

    LocalTime getCheckInTime();

    LocalTime getCheckOutTime();
}
