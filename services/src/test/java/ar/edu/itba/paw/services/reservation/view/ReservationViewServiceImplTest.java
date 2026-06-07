package ar.edu.itba.paw.services.reservation.view;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationDetailPageModel;
import ar.edu.itba.paw.policy.MoneyFormatPolicy;
import ar.edu.itba.paw.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.util.CarAvailabilityAddressFormatter;
import ar.edu.itba.paw.util.format.MoneyFormat;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationAvailabilityService;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.services.user.UserService;
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
    private CarService carService;

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private ReservationAvailabilityService reservationAvailabilityService;

    @Mock
    private CarPictureService carPictureService;

    @Mock
    private CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @Mock
    private PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReservationMessageService reservationMessageService;

    private MoneyFormat moneyFormat;

    private ReservationViewServiceImpl reservationViewService;

    @BeforeEach
    public void setUp() {
        moneyFormat = new MoneyFormat(
                MoneyFormatPolicy.fromValidatedConfiguration("ARS", "es-AR", 2, 2));
        reservationViewService = new ReservationViewServiceImpl(
                reservationService,
                carService,
                carAvailabilityService,
                reservationAvailabilityService,
                carPictureService,
                carAvailabilityAddressFormatter,
                userService,
                imageService,
                paymentReceiptUploadPolicy,
                reviewService,
                reservationMessageService,
                moneyFormat);
    }

    @Test
    public void testNormalizeReservationStatusQueryParamAcceptsWhitelistedLowercase() {
        Assertions.assertEquals("pending", reservationViewService.normalizeReservationStatusQueryParam("  PeNdIng  "));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam("bogus"));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam(""));
        Assertions.assertNull(reservationViewService.normalizeReservationStatusQueryParam(null));
    }

    @Test
    public void testToReservationCardDisplayRowUsesFrozenTotalPrice() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-16T18:00");
        final BigDecimal frozenTotal = BigDecimal.valueOf(200L);
        final ReservationCard card = new ReservationCard(
                1L,
                2L,
                "VW",
                "Gol",
                frozenTotal,
                3L,
                startDate,
                endDate,
                Reservation.Status.ACCEPTED);
        final var row = reservationViewService.toReservationCardDisplayRow(card, Locale.ENGLISH);
        Assertions.assertEquals(1L, row.getReservationId());
        Assertions.assertEquals("accepted", row.getStatusKey());
        Assertions.assertFalse(row.getPickupDateTime().isBlank());
        Assertions.assertFalse(row.getReturnDateTime().isBlank());
        Assertions.assertEquals(moneyFormat.format(frozenTotal), row.getTotalPrice());
    }


    @Test
    public void testLoadMyReservationDetailForViewerWhenReservationMissingReturnsEmpty() {
        Mockito.when(reservationService.getRiderReservationById(1L, 99L)).thenReturn(Optional.empty());
        final Optional<ReservationDetailPageModel> got =
                reservationViewService.loadMyReservationDetailForViewer(1L, 99L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isEmpty());
    }

    @Test
    public void testLoadMyReservationDetailForViewerWhenCarMissingReturnsEmpty() {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(7L);
        final Reservation res = Reservation.builder()
                .id(5L)
                .rider(User.identities(1L, "r@test.com", "R", "R"))
                .car(carRef)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationService.getRiderReservationById(1L, 5L)).thenReturn(Optional.of(res));
        Mockito.when(carService.getCarById(7L)).thenReturn(Optional.empty());
        final Optional<ReservationDetailPageModel> got =
                reservationViewService.loadMyReservationDetailForViewer(1L, 5L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isEmpty());
    }

    @Test
    public void testLoadReservationChatForParticipantWhenChatUnavailableReturnsEmpty() {
        final Car carRefPending = Mockito.mock(Car.class);
        final Reservation res = Reservation.builder()
                .id(5L)
                .rider(User.identities(1L, "r@test.com", "R", "R"))
                .car(carRefPending)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.PENDING)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationService.getRiderReservationById(1L, 5L)).thenReturn(Optional.of(res));
        Mockito.when(reservationMessageService.isChatAvailable(res)).thenReturn(false);
        final Optional<ReservationChatPageModel> got =
                reservationViewService.loadReservationChatForParticipant(1L, 5L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isEmpty());
    }

    @Test
    public void testLoadReservationChatForParticipantWhenChatAvailableReturnsModel() {
        final User owner = User.identities(9L, "o@test.com", "Owner", "One");
        owner.setProfilePicture(new Image(42L, "avatar.png", "image/png", new byte[] {1}));
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(7L);
        Mockito.when(carRef.getBrand()).thenReturn("VW");
        Mockito.when(carRef.getModel()).thenReturn("Gol");
        Mockito.when(carRef.getOwner()).thenReturn(owner);
        final Reservation res = Reservation.builder()
                .id(5L)
                .rider(User.identities(1L, "r@test.com", "R", "R"))
                .car(carRef)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationService.getRiderReservationById(1L, 5L)).thenReturn(Optional.of(res));
        Mockito.when(reservationMessageService.isChatAvailable(res)).thenReturn(true);
        Mockito.when(carService.getCarById(7L)).thenReturn(Optional.of(carRef));
        Mockito.when(userService.getUserById(9L)).thenReturn(Optional.of(owner));
        Mockito.when(reservationMessageService.getMessageBodyMaxLength()).thenReturn(1000);
        Mockito.when(reservationMessageService.getMaxChatAttachmentMegabytes()).thenReturn(25);
        Mockito.when(reservationMessageService.getHistoryPageSize()).thenReturn(50);
        final Optional<ReservationChatPageModel> got =
                reservationViewService.loadReservationChatForParticipant(1L, 5L, "rider", Locale.ENGLISH);
        Assertions.assertTrue(got.isPresent());
        Assertions.assertEquals(5L, got.get().getReservationId());
        Assertions.assertEquals(7L, got.get().getCarId());
        Assertions.assertEquals(42L, got.get().getCounterpartyProfileImageId());
    }
}
