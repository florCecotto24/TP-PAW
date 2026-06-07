package ar.edu.itba.paw.webapp.form.car;

import java.time.LocalTime;

/**
 * Shared shape for listing publish/edit forms validated by {@code @CheckOutAfterCheckIn}.
 */
public interface CarAvailabilityTimeWindow {

    LocalTime getCheckInTime();

    LocalTime getCheckOutTime();
}
