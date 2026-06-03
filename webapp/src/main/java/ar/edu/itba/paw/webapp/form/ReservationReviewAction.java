package ar.edu.itba.paw.webapp.form;

/**
 * Which control submitted the mutual review form ({@linkplain ReservationReviewForm}).
 *
 * <p>Only {@link #SUBMIT} is wired to a button today; the OMIT path used to be triggered by a second
 * submit button that has since been removed. Stale reviews are now auto-skipped by a scheduled job.
 */
public enum ReservationReviewAction {
    SUBMIT
}
