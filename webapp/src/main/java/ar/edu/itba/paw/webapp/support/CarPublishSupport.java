package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarPublishingService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.CarDto;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * HTTP binding for {@code POST /cars}: JSON or multipart publish, validation, and Location.
 * Keeps {@code CarController} free of stream/multipart plumbing.
 */
@Component
public final class CarPublishSupport {

    private final CarService carService;
    private final CarPublishingService carPublishingService;
    private final CarCreateRequestSupport carCreateRequestSupport;
    private final FormValidationSupport formValidationSupport;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final ObjectMapper objectMapper;

    public CarPublishSupport(
            final CarService carService,
            final CarPublishingService carPublishingService,
            final CarCreateRequestSupport carCreateRequestSupport,
            final FormValidationSupport formValidationSupport,
            final BinaryPayloadSupport binaryPayloadSupport,
            final ObjectMapper objectMapper) {
        this.carService = carService;
        this.carPublishingService = carPublishingService;
        this.carCreateRequestSupport = carCreateRequestSupport;
        this.formValidationSupport = formValidationSupport;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.objectMapper = objectMapper;
    }

    public Response publishJson(final long ownerId, final CarCreateForm form, final Locale locale, final UriInfo uriInfo)
            throws IOException {
        return publish(ownerId, form, List.of(), null, null, null, locale, uriInfo);
    }

    public Response publishMultipart(
            final long ownerId,
            final InputStream carPart,
            final List<FormDataBodyPart> pictureParts,
            final FormDataBodyPart insurancePart,
            final Locale locale,
            final UriInfo uriInfo) throws IOException {
        final OptionalInsurance insurance = readOptionalInsurance(insurancePart);
        return publish(
                ownerId,
                readCarCreateForm(carPart),
                readGalleryUploads(pictureParts),
                insurance.fileName,
                insurance.contentType,
                insurance.bytes,
                locale,
                uriInfo);
    }

    private Response publish(
            final long ownerId,
            final CarCreateForm form,
            final List<GalleryMediaUpload> galleryUploads,
            final String insuranceName,
            final String insuranceType,
            final byte[] insuranceBytes,
            final Locale locale,
            final UriInfo uriInfo) throws IOException {
        formValidationSupport.validate(form, ValidationGroups.OnPublishCar.class);

        final PublishCarRequest fullRequest = carCreateRequestSupport.toPublishRequest(
                form, galleryUploads, insuranceName, insuranceType, insuranceBytes);

        final PublishCarOutcome outcome = carPublishingService.publishCar(ownerId, fullRequest, locale);
        final Car car = outcome.getCar();
        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars")
                .path(String.valueOf(car.getId()))
                .build();
        final Car refreshed = carService.getCarById(car.getId()).orElse(car);
        return Response.created(location)
                .entity(CarDto.fromPrivate(refreshed, uriInfo))
                .type(VndMediaType.CAR_PRIVATE_V1_JSON)
                .build();
    }

    private CarCreateForm readCarCreateForm(final InputStream carPart) throws IOException {
        final byte[] carJson = binaryPayloadSupport.readValidatedBody(carPart);
        return objectMapper.readValue(carJson, CarCreateForm.class);
    }

    private List<GalleryMediaUpload> readGalleryUploads(final List<FormDataBodyPart> pictureParts)
            throws IOException {
        if (pictureParts == null || pictureParts.isEmpty()) {
            return List.of();
        }
        final List<GalleryMediaUpload> uploads = new ArrayList<>();
        for (final FormDataBodyPart part : pictureParts) {
            if (part == null) {
                continue;
            }
            final InputStream stream = part.getEntityAs(InputStream.class);
            if (stream == null) {
                continue;
            }
            final byte[] bytes = binaryPayloadSupport.readBounded(stream);
            if (bytes.length == 0) {
                continue;
            }
            final String filename = part.getContentDisposition() != null
                    ? part.getContentDisposition().getFileName()
                    : "upload";
            final String contentType = part.getMediaType() != null
                    ? part.getMediaType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM;
            uploads.add(new GalleryMediaUpload(filename, contentType, bytes));
        }
        return uploads;
    }

    private OptionalInsurance readOptionalInsurance(final FormDataBodyPart insurancePart) throws IOException {
        if (insurancePart == null) {
            return OptionalInsurance.empty();
        }
        final InputStream insuranceStream = insurancePart.getEntityAs(InputStream.class);
        if (insuranceStream == null) {
            return OptionalInsurance.empty();
        }
        final byte[] insuranceBytes = binaryPayloadSupport.readValidatedBody(insuranceStream);
        final String insuranceName = insurancePart.getContentDisposition() != null
                ? insurancePart.getContentDisposition().getFileName()
                : "insurance";
        final String insuranceType = insurancePart.getMediaType() != null
                ? insurancePart.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        return new OptionalInsurance(insuranceName, insuranceType, insuranceBytes);
    }

    private record OptionalInsurance(String fileName, String contentType, byte[] bytes) {
        private static OptionalInsurance empty() {
            return new OptionalInsurance(null, null, null);
        }
    }
}
