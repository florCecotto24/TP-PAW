package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.models.email.ReservationConfirmationEmailPayload;
import ar.edu.itba.paw.services.mail.MailPublicUrls;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    private record SentMail(String to, String subject) {
    }

    private static final String TEMPLATE_RIDER = "html/reservation-confirmation-rider";
    private static final String TEMPLATE_OWNER = "html/reservation-confirmation-owner";
    private static final String SUBJECT_KEY = "mail.reservationRequestSent.subject";

    @Mock
    private Environment environment;

    @Mock
    private MailPublicUrls mailPublicUrls;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource emailMessageSource;

    @Mock
    private TemplateEngine htmlTemplateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    private static Answer<Void> recordSentInto(final List<SentMail> out) {
        return invocation -> {
            final MimeMessage message = invocation.getArgument(0);
            try {
                out.add(new SentMail(
                        ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress(),
                        message.getSubject()));
            } catch (final MessagingException e) {
                throw new AssertionError(e);
            }
            return null;
        };
    }

    @Test
    public void testSendReservationConfirmationEmail() {
        // 1. Arrange
        Mockito.when(mailPublicUrls.absolutePath(Mockito.anyString()))
                .thenAnswer(invocation -> "http://localhost" + invocation.getArgument(0));
        final List<SentMail> sent = new ArrayList<>();
        Mockito.doAnswer(recordSentInto(sent)).when(mailSender).send(Mockito.any(MimeMessage.class));
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getDefaultInstance(new Properties())));

        final Locale locale = Locale.ENGLISH;
        final String riderEmail = "rider@test.com";
        final String riderName = "Rider Name";
        final long reservationId = 1L;
        final long listingId = 10L;
        final String vehicleLabel = "brandTest modelTest";
        final OffsetDateTime startDate = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        final OffsetDateTime endDate = OffsetDateTime.parse("2026-05-03T18:00:00Z");
        final String riderHandoverLocation = "pickupStreetTest";
        final String ownerName = "Owner Name";
        final String ownerEmail = "owner@test.com";
        final String subject = "Subject brandTest modelTest";
        final String reservationTotal = "100.00";
        final String ownerCbu = "1234567890123456789012";
        final ReservationConfirmationEmailPayload payload = ReservationConfirmationEmailPayload.builder()
                .recipientEmail(riderEmail)
                .riderFullName(riderName)
                .reservationId(reservationId)
                .listingId(listingId)
                .vehicleLabel(vehicleLabel)
                .startDate(startDate)
                .endDate(endDate)
                .riderHandoverLocation(riderHandoverLocation)
                .ownerHandoverLocation(riderHandoverLocation + " (owner full)")
                .ownerFullName(ownerName)
                .ownerEmail(ownerEmail)
                .reservationTotal(reservationTotal)
                .riderMailLocale(locale)
                .ownerMailLocale(locale)
                .ownerCbu(ownerCbu)
                .build();

        Mockito.when(environment.getProperty("mail.server.username", "noreply@localhost"))
                .thenReturn("noreply@localhost");
        Mockito.when(environment.getProperty("mail.from.address", "noreply@localhost"))
                .thenReturn("noreply@app.test");
        Mockito.when(emailMessageSource.getMessage(Mockito.eq(SUBJECT_KEY), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn(subject);
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_RIDER), Mockito.any(Context.class)))
                .thenReturn("<html>rider</html>");
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_OWNER), Mockito.any(Context.class)))
                .thenReturn("<html>owner</html>");

        // 2. Execute
        emailService.sendReservationConfirmationEmail(payload);

        // 3. Assert
        Assertions.assertEquals(2, sent.size());
        Assertions.assertEquals(riderEmail, sent.get(0).to());
        Assertions.assertEquals(subject, sent.get(0).subject());
        Assertions.assertEquals(ownerEmail, sent.get(1).to());
        Assertions.assertEquals(subject, sent.get(1).subject());
    }


}
