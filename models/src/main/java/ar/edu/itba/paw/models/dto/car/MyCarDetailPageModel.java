package ar.edu.itba.paw.models.dto.car;

import ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bundle returned by {@code MyCarDetailViewService.loadMyCarDetailPage} for the
 * {@code car/myCarDetail.jsp} view used by both {@code MyCarsController.myCarDetail} and the
 * error-rerender path inside {@code MyCarsController.editCar}.
 *
 * <p>Has two flavors:</p>
 * <ul>
 *   <li><b>Rich</b> — the owner-detail page model + the top-3 preview reservations + the total
 *       count. Built when {@code carService.buildOwnerCarDetailPageModel} returns present.</li>
 *   <li><b>Fallback</b> — only the first car-picture image id +
 *       {@code hasPublishedAvailability=false}. Built when the rich model is absent (e.g. the car
 *       has no published availability yet); the controller already holds the {@code Car}.</li>
 * </ul>
 *
 * <p>The view's JSP-side attributes have not changed; the controller still adds
 * {@code editForm} and {@code activeTab} (and the appropriate {@code page}/{@code error}
 * decorations of its own). This DTO simply packages the heavy reads.</p>
 */
public final class MyCarDetailPageModel {

    public enum Variant { RICH, FALLBACK }

    private final Variant variant;
    private final OwnerCarDetailPageModel ownerPageModelOrNull;
    private final List<ReservationCardDisplayRow> previewReservations;
    private final long reservationPreviewTotal;
    private final long fallbackCarImageId;

    private MyCarDetailPageModel(
            final Variant variant,
            final OwnerCarDetailPageModel ownerPageModelOrNull,
            final List<ReservationCardDisplayRow> previewReservations,
            final long reservationPreviewTotal,
            final long fallbackCarImageId) {
        this.variant = Objects.requireNonNull(variant, "variant");
        this.ownerPageModelOrNull = ownerPageModelOrNull;
        this.previewReservations = previewReservations == null ? List.of() : List.copyOf(previewReservations);
        this.reservationPreviewTotal = reservationPreviewTotal;
        this.fallbackCarImageId = fallbackCarImageId;
    }

    public static MyCarDetailPageModel rich(
            final OwnerCarDetailPageModel ownerPageModel,
            final List<ReservationCardDisplayRow> previewReservations,
            final long reservationPreviewTotal) {
        return new MyCarDetailPageModel(
                Variant.RICH,
                Objects.requireNonNull(ownerPageModel, "ownerPageModel"),
                previewReservations,
                reservationPreviewTotal,
                0L);
    }

    public static MyCarDetailPageModel fallback(final long firstImageId) {
        return new MyCarDetailPageModel(Variant.FALLBACK, null, List.of(), 0L, firstImageId);
    }

    public Variant getVariant() { return variant; }

    public Optional<OwnerCarDetailPageModel> getOwnerPageModel() {
        return Optional.ofNullable(ownerPageModelOrNull);
    }

    public List<ReservationCardDisplayRow> getPreviewReservations() { return previewReservations; }

    public long getReservationPreviewTotal() { return reservationPreviewTotal; }

    public long getFallbackCarImageId() { return fallbackCarImageId; }
}
