package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyReservationsControllerTest {

    @Test
    public void counterpartyProfile_ownerCounterpartyShowsActiveListingsExcludingCurrentListing() {
        final ReservationService reservationService = mock(ReservationService.class);
        final ListingService listingService = mock(ListingService.class);
        final ImageService imageService = mock(ImageService.class);
        final LocaleMessages localeMessages = new LocaleMessages(mock(MessageSource.class));
        final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy = paymentReceiptUploadPolicy();
        final ReviewService reviewService = mock(ReviewService.class);
        final UserService userService = mock(UserService.class);
        final PresentationLimitsPolicy presentationLimitsPolicy = presentationLimitsPolicy();
        final MyReservationsController controller = new MyReservationsController(
                reservationService,
                listingService,
                imageService,
                localeMessages,
                paymentReceiptUploadPolicy,
                reviewService,
                userService,
                presentationLimitsPolicy);

        final long meId = 100L;
        final long reservationId = 200L;
        final long listingId = 300L;
        final long ownerId = 400L;
        final User me = user(meId, "Me", "User");
        final Reservation reservation = reservation(reservationId, 500L, listingId);
        final User owner = user(ownerId, "Owner", "Host");

        when(reservationService.getRiderReservationById(meId, reservationId)).thenReturn(Optional.of(reservation));
        when(userService.getListingOwner(listingId)).thenReturn(Optional.of(owner));
        when(reviewService.getAverageRatingForCounterparty(ownerId, true)).thenReturn(BigDecimal.valueOf(4.8));
        when(reviewService.getRecentCommentReviewsForCounterparty(ownerId, true, 3))
                .thenReturn(List.of(new ReviewItemDto(11L, "Ana", null, 5, LocalDate.of(2026, 1, 1), "Great")));
        final OwnerListingSearchCriteria ownerCriteria = new OwnerListingSearchCriteria(
                ownerId,
                0,
                PaginationFallbackSizes.UI_PAGE_SIZE,
                List.of("active"),
                null,
                null,
                null,
                null,
                null,
                "date",
                "desc");
        when(listingService.buildOwnerListingSearchCriteria(ownerId, null, null, null, null, List.of("active"), null, 0, null))
                .thenReturn(ownerCriteria);
        when(listingService.getOwnerListingCards(ownerCriteria))
                .thenReturn(new Page<>(List.of(
                        new ListingCard(listingId, "Toyota", "Etios", BigDecimal.valueOf(100), 1L),
                        new ListingCard(301L, "Ford", "Fiesta", BigDecimal.valueOf(120), 2L)
                ), 0, PaginationFallbackSizes.UI_PAGE_SIZE, 2));

        final ModelAndView mav = controller.counterpartyProfile(me, reservationId, "rider");

        assertEquals("counterpartyProfile", mav.getViewName());
        assertTrue((Boolean) mav.getModel().get("showCounterpartyActiveListings"));
        assertFalse((Boolean) mav.getModel().get("counterpartyLicenseValidated"));
        assertFalse((Boolean) mav.getModel().get("counterpartyInsuranceValidated"));
        assertFalse((Boolean) mav.getModel().get("counterpartyIdentityValidated"));
        @SuppressWarnings("unchecked")
        final List<VehicleCardView> cards = (List<VehicleCardView>) mav.getModel().get("counterpartyActiveListings");
        assertEquals(1, cards.size());
        assertEquals(301L, cards.get(0).getListingId());
        verify(listingService).buildOwnerListingSearchCriteria(ownerId, null, null, null, null, List.of("active"), null, 0, null);
    }

    @Test
    public void counterpartyProfile_riderCounterpartyDoesNotShowActiveListings() {
        final ReservationService reservationService = mock(ReservationService.class);
        final ListingService listingService = mock(ListingService.class);
        final ImageService imageService = mock(ImageService.class);
        final LocaleMessages localeMessages = new LocaleMessages(mock(MessageSource.class));
        final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy = paymentReceiptUploadPolicy();
        final ReviewService reviewService = mock(ReviewService.class);
        final UserService userService = mock(UserService.class);
        final PresentationLimitsPolicy presentationLimitsPolicy = presentationLimitsPolicy();
        final MyReservationsController controller = new MyReservationsController(
                reservationService,
                listingService,
                imageService,
                localeMessages,
                paymentReceiptUploadPolicy,
                reviewService,
                userService,
                presentationLimitsPolicy);

        final long meId = 700L;
        final long reservationId = 800L;
        final long listingId = 900L;
        final long riderId = 1000L;
        final User me = user(meId, "Me", "Owner");
        final Reservation reservation = reservation(reservationId, riderId, listingId);
        final User rider = user(riderId, "Rider", "Guest");

        when(reservationService.getOwnerReservationById(meId, reservationId)).thenReturn(Optional.of(reservation));
        when(userService.getUserById(riderId)).thenReturn(Optional.of(rider));
        when(reviewService.getAverageRatingForCounterparty(riderId, false)).thenReturn(BigDecimal.valueOf(4.2));
        when(reviewService.getRecentCommentReviewsForCounterparty(riderId, false, 3)).thenReturn(List.of());

        final ModelAndView mav = controller.counterpartyProfile(me, reservationId, "owner");

        assertEquals("counterpartyProfile", mav.getViewName());
        assertFalse((Boolean) mav.getModel().get("showCounterpartyActiveListings"));
        assertFalse((Boolean) mav.getModel().get("counterpartyLicenseValidated"));
        assertFalse((Boolean) mav.getModel().get("counterpartyInsuranceValidated"));
        assertFalse((Boolean) mav.getModel().get("counterpartyIdentityValidated"));
        @SuppressWarnings("unchecked")
        final List<VehicleCardView> cards = (List<VehicleCardView>) mav.getModel().get("counterpartyActiveListings");
        assertTrue(cards.isEmpty());
        verify(listingService, never()).buildOwnerListingSearchCriteria(1000L, null, null, null, null, List.of("active"), null, 0, null);
    }

    private static User user(final long id, final String forename, final String surname) {
        return User.builder()
                .id(id)
                .email(forename.toLowerCase() + "@example.com")
                .forename(forename)
                .surname(surname)
                .memberSince(LocalDate.of(2024, 1, 1))
                .build();
    }

    private static Reservation reservation(final long id, final long riderId, final long listingId) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Reservation.builder()
                .id(id)
                .riderId(riderId)
                .listingId(listingId)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(2))
                .status(Reservation.Status.ACCEPTED)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .totalPrice(BigDecimal.valueOf(1000))
                .build();
    }

    private static PaymentReceiptUploadPolicy paymentReceiptUploadPolicy() {
        return new PaymentReceiptUploadPolicy(new MockEnvironment()
                .withProperty("app.upload.max-payment-receipt-megabytes", "5")
                .withProperty("app.upload.bytes-per-binary-megabyte", "1048576"));
    }

    private static PresentationLimitsPolicy presentationLimitsPolicy() {
        return new PresentationLimitsPolicy(new MockEnvironment()
                .withProperty("app.presentation.counterparty-recent-reviews-limit", "3")
                .withProperty("app.presentation.car-detail-similar-listings-limit", "4"));
    }

}
