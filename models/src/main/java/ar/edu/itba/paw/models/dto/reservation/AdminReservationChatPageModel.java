package ar.edu.itba.paw.models.dto.reservation;


import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;

/**
 * Page model consumed by {@code admin/reservationChat.jsp}. Built by
 * {@code AdminService.loadReservationChatPage}.
 *
 * <p>Before this DTO the {@code AdminController.reservationChat} handler made three separate
 * calls to {@code AdminService} (get reservation, list messages, count messages) and added five
 * attributes to the model. With this page model the controller only renders the view.</p>
 */
public final class AdminReservationChatPageModel {

    private final Reservation reservation;
    private final List<ReservationMessage> messages;
    private final long totalMessages;
    private final int currentPage;
    private final int pageSize;

    public AdminReservationChatPageModel(
            final Reservation reservation,
            final List<ReservationMessage> messages,
            final long totalMessages,
            final int currentPage,
            final int pageSize) {
        this.reservation = Objects.requireNonNull(reservation, "reservation");
        this.messages = List.copyOf(messages);
        this.totalMessages = totalMessages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public Reservation getReservation() { return reservation; }

    public List<ReservationMessage> getMessages() { return messages; }

    public long getTotalMessages() { return totalMessages; }

    public int getCurrentPage() { return currentPage; }

    public int getPageSize() { return pageSize; }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("reservation", reservation);
        sink.accept("messages", messages);
        sink.accept("totalMessages", totalMessages);
        sink.accept("currentPage", currentPage);
        sink.accept("pageSize", pageSize);
    }
}
