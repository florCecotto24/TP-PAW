package ar.edu.itba.paw.webapp.controller;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyReservationsControllerTest {

    @Test
    public void testCounterpartyProfileWhenServiceReturnsEmpty() {
        final ReservationService reservationService = mock(ReservationService.class);
        final ReservationViewService reservationViewService = mock(ReservationViewService.class);
        final ListingService listingService = mock(ListingService.class);
        final LocaleMessages localeMessages = new LocaleMessages(mock(MessageSource.class));
        final ReviewService reviewService = mock(ReviewService.class);
        final MyReservationsController controller = new MyReservationsController(
                reservationService, reservationViewService, listingService, localeMessages, reviewService);

        final User me = User.builder().id(1L).email("a@b.com").forename("A").surname("B").build();
        when(reservationViewService.loadCounterpartyProfileForReservationParticipant(
                eq(1L), eq(99L), eq("rider"), any(Locale.class)))
                .thenReturn(Optional.empty());

        final ModelAndView mav = controller.counterpartyProfile(me, 99L, "rider");

        assertInstanceOf(RedirectView.class, mav.getView());
        final RedirectView rv = (RedirectView) mav.getView();
        assertTrue(rv.getUrl().contains("/my-reservations/99"));
        assertTrue(rv.getUrl().contains("role=rider"));
    }

    @Test
    public void testCounterpartyProfileWhenServiceReturnsModel() {
        final ReservationService reservationService = mock(ReservationService.class);
        final ReservationViewService reservationViewService = mock(ReservationViewService.class);
        final ListingService listingService = mock(ListingService.class);
        final LocaleMessages localeMessages = new LocaleMessages(mock(MessageSource.class));
        final ReviewService reviewService = mock(ReviewService.class);
        final MyReservationsController controller = new MyReservationsController(
                reservationService, reservationViewService, listingService, localeMessages, reviewService);

        final User me = User.builder().id(1L).email("a@b.com").forename("A").surname("B").build();
        final CounterpartyProfilePageModel model = new CounterpartyProfilePageModel(
                "Ann",
                "Owner",
                "",
                null,
                null,
                BigDecimal.valueOf(4.5),
                false,
                false,
                List.of(),
                false,
                List.of());
        when(reservationViewService.loadCounterpartyProfileForReservationParticipant(
                anyLong(), anyLong(), eq("owner"), any(Locale.class)))
                .thenReturn(Optional.of(model));

        final ModelAndView mav = controller.counterpartyProfile(me, 5L, "owner");

        assertEquals("counterpartyProfile", mav.getViewName());
        assertEquals("my-reservations", mav.getModel().get("activeTab"));
        assertEquals("Ann", mav.getModel().get("counterpartyForename"));
        assertEquals("Owner", mav.getModel().get("counterpartySurname"));
    }
}
