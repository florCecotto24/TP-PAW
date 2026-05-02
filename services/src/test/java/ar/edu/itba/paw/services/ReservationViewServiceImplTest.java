package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;

@ExtendWith(MockitoExtension.class)
public class ReservationViewServiceImplTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-05T18:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("200");

    @Mock
    private ReservationService reservationService;

    @Mock
    private ListingService listingService;

    @Mock
    private ListingViewService listingViewService;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @Mock
    private PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;

    @Mock
    private ReviewService reviewService;

    @Mock
    private PresentationLimitsPolicy presentationLimitsPolicy;

    @InjectMocks
    private ReservationViewServiceImpl reservationViewService;

    @Test
    public void testNormalizeReservationStatusQueryParamAcceptsWhitelistedLowercase() {
        Assertions.assertEquals("pending", reservationViewService.normalizeReservationStatusQueryParam("  PeNdIng  "));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam("bogus"));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam(""));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam(null));
    }

    @Test
    public void testToReservationCardDisplayRowUsesBillableDaysAndDayPriceForTotal() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-16T18:00");
        final ReservationCard card = new ReservationCard(
                1L,
                2L,
                "VW",
                "Gol",
                BigDecimal.valueOf(100L),
                3L,
                startDate,
                endDate,
                Reservation.Status.ACCEPTED);
        Mockito.when(reservationService.calculateBillableDays(startDate, endDate)).thenReturn(2L);
        final var row = reservationViewService.toReservationCardDisplayRow(card, Locale.ENGLISH);
        Assertions.assertEquals(1L, row.getReservationId());
        Assertions.assertEquals("accepted", row.getStatusKey());
        Assertions.assertFalse(row.getPickupDateTime().isBlank());
        Assertions.assertFalse(row.getReturnDateTime().isBlank());
        Assertions.assertEquals(ArsMoneyFormat.format(BigDecimal.valueOf(100L).multiply(BigDecimal.valueOf(2L))), row.getTotalPrice());
    }

    @Test
    public void testLoadMyReservationDetailForViewerWhenReservationMissingReturnsEmpty() {
        Mockito.when(reservationService.getRiderReservationById(1L, 99L)).thenReturn(Optional.empty());
        final Optional<ReservationDetailPageModel> got =
                reservationViewService.loadMyReservationDetailForViewer(1L, 99L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isEmpty());
    }

    @Test
    public void testLoadMyReservationDetailForViewerWhenListingDetailMissingReturnsEmpty() {
        final Reservation res = Reservation.builder()
                .id(5L)
                .riderId(1L)
                .listingId(2L)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationService.getRiderReservationById(1L, 5L)).thenReturn(Optional.of(res));
        Mockito.when(listingService.getListingDetailById(2L)).thenReturn(Optional.empty());
        final Optional<ReservationDetailPageModel> got =
                reservationViewService.loadMyReservationDetailForViewer(1L, 5L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isEmpty());
    }
}
