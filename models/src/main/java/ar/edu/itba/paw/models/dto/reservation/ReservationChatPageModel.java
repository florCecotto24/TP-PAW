package ar.edu.itba.paw.models.dto.reservation;

import java.util.function.BiConsumer;

/**
 * Model attributes for the {@code reservationChat} JSP; built in the service layer and applied via
 * {@link #populateModel(BiConsumer)}.
 */
public final class ReservationChatPageModel {

    private final long reservationId;
    private final long carId;
    private final String vehicleLabel;
    private final String reservationRole;
    private final String counterpartyDisplayName;
    private final Long counterpartyProfileImageId;
    private final long chatViewerUserId;
    private final int chatMessageMaxLength;
    private final int chatMaxAttachmentMegabytes;

    public ReservationChatPageModel(
            final long reservationId,
            final long carId,
            final String vehicleLabel,
            final String reservationRole,
            final String counterpartyDisplayName,
            final Long counterpartyProfileImageId,
            final long chatViewerUserId,
            final int chatMessageMaxLength,
            final int chatMaxAttachmentMegabytes) {
        this.reservationId = reservationId;
        this.carId = carId;
        this.vehicleLabel = vehicleLabel;
        this.reservationRole = reservationRole;
        this.counterpartyDisplayName = counterpartyDisplayName;
        this.counterpartyProfileImageId = counterpartyProfileImageId;
        this.chatViewerUserId = chatViewerUserId;
        this.chatMessageMaxLength = chatMessageMaxLength;
        this.chatMaxAttachmentMegabytes = chatMaxAttachmentMegabytes;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getCarId() {
        return carId;
    }

    public Long getCounterpartyProfileImageId() {
        return counterpartyProfileImageId;
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("reservationId", reservationId);
        putObject.accept("carId", carId);
        putObject.accept("vehicleLabel", vehicleLabel);
        putObject.accept("reservationRole", reservationRole);
        putObject.accept("counterpartyDisplayName", counterpartyDisplayName);
        putObject.accept("counterpartyProfileImageId", counterpartyProfileImageId);
        putObject.accept("chatViewerUserId", chatViewerUserId);
        putObject.accept("chatMessageMaxLength", chatMessageMaxLength);
        putObject.accept("chatMaxAttachmentMegabytes", chatMaxAttachmentMegabytes);
    }
}
