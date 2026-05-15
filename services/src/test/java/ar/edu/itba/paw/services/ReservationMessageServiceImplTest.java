package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.services.policy.ReservationChatPolicy;
import ar.edu.itba.paw.services.policy.ReservationMessageValidationPolicy;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationMessageServiceImplTest {

    private static final long OWNER_ID = 10L;
    private static final long RIDER_ID = 20L;
    private static final long RESERVATION_ID = 30L;
    private static final long LISTING_ID = 40L;

    @Mock
    private ReservationMessageDao reservationMessageDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @Mock
    private ListingService listingService;

    @Mock
    private CarService carService;

    @Mock
    private EmailService emailService;

    @Mock
    private MailPublicUrls mailPublicUrls;

    private ReservationMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationMessageServiceImpl(
                reservationMessageDao,
                reservationService,
                userService,
                listingService,
                carService,
                emailService,
                mailPublicUrls,
                ReservationMessageValidationPolicy.fromValidatedBodyMaxLength(1000),
                ReservationChatPolicy.fromValidatedConfiguration(7, 50));
    }

    private static Reservation reservation(final Reservation.Status status, final boolean paymentApproved) {
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(LISTING_ID);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .listing(listingRef)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(2))
                .status(status)
                .createdAt(now.minusDays(5))
                .updatedAt(now)
                .totalPrice(new BigDecimal("100"))
                .paymentApproved(paymentApproved)
                .build();
    }

    @Test
    void testIsChatAvailableWhenAcceptedAndPaymentApproved() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED, true);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(service.isChatAvailable(res));
    }

    @Test
    void testIsChatAvailableWhenPending() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.PENDING, true);

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(service.isChatAvailable(res));
    }

    @Test
    void testIsChatAvailableWhenFinishedBeyondGracePeriod() {
        // 1.Arrange
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(LISTING_ID);
        final Reservation res = Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .listing(listingRef)
                .startDate(now.minusDays(20))
                .endDate(now.minusDays(10))
                .status(Reservation.Status.FINISHED)
                .createdAt(now.minusDays(25))
                .updatedAt(now)
                .totalPrice(new BigDecimal("100"))
                .paymentApproved(true)
                .build();

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(service.isChatAvailable(res));
    }

    @Test
    void testPostMessageRejectsEmptyBody() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED, true);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Exercise & 3.Assert
        final ReservationMessageException ex = Assertions.assertThrows(
                ReservationMessageException.class,
                () -> service.postMessage(RIDER_ID, RESERVATION_ID, "   "));
        Assertions.assertEquals(MessageKeys.RESERVATION_CHAT_BODY_EMPTY, ex.getMessageCode());
    }
}
