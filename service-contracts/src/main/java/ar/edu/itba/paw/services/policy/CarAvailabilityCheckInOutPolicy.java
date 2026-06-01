package ar.edu.itba.paw.services.policy;

import java.time.LocalTime;

/** Minimum gap between a listing's check-in and check-out times. */
public interface CarAvailabilityCheckInOutPolicy {

    int getMinHoursBetweenCheckInAndCheckOut();

    /**
     * Same calendar-day window as {@code @CheckOutAfterCheckIn}: checkout strictly after check-in.
     */
    boolean hasMinimumGap(LocalTime checkInTime, LocalTime checkOutTime);
}
