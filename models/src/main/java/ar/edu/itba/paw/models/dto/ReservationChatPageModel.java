package ar.edu.itba.paw.models.dto;

import java.util.function.BiConsumer;

/**
 * Model attributes for the {@code reservationChat} JSP; built in the service layer and applied via
 * {@link #populateModel(BiConsumer)}.
 */
public final class ReservationChatPageModel {

    private final long reservationId;
    private final long listingId;
    private final String listingTitle;
    private final String reservationRole;
    private final String counterpartyDisplayName;
    private final Long counterpartyProfileImageId;
    private final long chatViewerUserId;
    private final int chatMessageMaxLength;
    private final int chatMaxAttachmentMegabytes;

    public ReservationChatPageModel(
            final long reservationId,
            final long listingId,
            final String listingTitle,
            final String reservationRole,
            final String counterpartyDisplayName,
            final Long counterpartyProfileImageId,
            final long chatViewerUserId,
            final int chatMessageMaxLength,
            final int chatMaxAttachmentMegabytes) {
        this.reservationId = reservationId;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
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

    public long getListingId() {
        return listingId;
    }

    public Long getCounterpartyProfileImageId() {
        return counterpartyProfileImageId;
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("reservationId", reservationId);
        putObject.accept("listingId", listingId);
        putObject.accept("listingTitle", listingTitle);
        putObject.accept("reservationRole", reservationRole);
        putObject.accept("counterpartyDisplayName", counterpartyDisplayName);
        putObject.accept("counterpartyProfileImageId", counterpartyProfileImageId);
        putObject.accept("chatViewerUserId", chatViewerUserId);
        putObject.accept("chatMessageMaxLength", chatMessageMaxLength);
        putObject.accept("chatMaxAttachmentMegabytes", chatMaxAttachmentMegabytes);
    }
}
