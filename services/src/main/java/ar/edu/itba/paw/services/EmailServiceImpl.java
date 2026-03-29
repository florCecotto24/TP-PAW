package ar.edu.itba.paw.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Autowired
    public EmailServiceImpl(final JavaMailSender mailSender, @Value("${mail.from}") final String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void notifyListingOwnerNewReservation(
            final String ownerEmail,
            final String listingTitle,
            final long reservationId,
            final String riderEmail) {
        final String subject = "Nueva reserva en tu publicación";
        final String text = "Se registró una reserva (id " + reservationId + ") para la publicación \""
                + listingTitle + "\".\n"
                + "Solicitante (email de contacto): " + riderEmail + ".";
        send(ownerEmail, subject, text);
    }

    @Override
    public void notifyRiderReservationDetails(
            final String riderEmail,
            final String riderFullName,
            final long reservationId,
            final String listingTitle,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final String deliveryLocation,
            final String ownerName,
            final String ownerEmail) {
        final String subject = "Confirmación de reserva — " + listingTitle;
        final String safeLocation = deliveryLocation == null || deliveryLocation.isBlank() ? "No indicada" : deliveryLocation;
        final String text = "Hola " + riderFullName + ",\n\n"
                + "Tu reserva quedó registrada.\n\n"
                + "Número de reserva: " + reservationId + "\n"
                + "Vehículo / publicación: " + listingTitle + "\n"
                + "Inicio: " + startDate.format(DATE_TIME_FMT) + "\n"
                + "Fin: " + endDate.format(DATE_TIME_FMT) + "\n"
                + "Lugar de entrega indicado: " + safeLocation + "\n\n"
                + "Datos del dueño para coordinar:\n"
                + "Nombre: " + ownerName + "\n"
                + "Email: " + ownerEmail + "\n\n"
                + "Podés contactar al dueño por ese correo para acordar la entrega del vehículo.\n";
        send(riderEmail, subject, text);
    }

    private void send(final String to, final String subject, final String text) {
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (final Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo a " + to, e);
        }
    }
}
