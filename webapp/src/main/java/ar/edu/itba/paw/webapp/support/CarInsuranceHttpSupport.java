package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.support.facade.CarInsuranceUploadFacade;

/**
 * HTTP binding for {@code /cars/{id}/insurance}: download, upload outcome mapping, clear.
 */
@Component
public final class CarInsuranceHttpSupport {

    private final CarService carService;
    private final StoredFileService storedFileService;
    private final CarInsuranceUploadFacade carInsuranceUploadFacade;
    private final BinaryPayloadSupport binaryPayloadSupport;

    public CarInsuranceHttpSupport(
            final CarService carService,
            final StoredFileService storedFileService,
            final CarInsuranceUploadFacade carInsuranceUploadFacade,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.storedFileService = storedFileService;
        this.carInsuranceUploadFacade = carInsuranceUploadFacade;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    public Response download(final long carId) {
        final Car car = requireCarExists(carId);
        final Long fileId = car.getInsuranceFileId().orElseThrow(NotFoundException::new);
        return storedFileService.findContentById(fileId)
                .map(this::sensitiveBinary)
                .orElseThrow(NotFoundException::new);
    }

    public Response upload(final long carId, final InputStream body, final HttpHeaders httpHeaders)
            throws IOException {
        final Car car = requireCarExists(carId);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final String filename = resolveUploadFileName(httpHeaders, contentType);
        final var outcome = carInsuranceUploadFacade.attemptUploadFromBytes(
                car.getOwnerId(), carId, filename, contentType, bytes);
        return switch (outcome.getStatus()) {
            case OK -> Response.noContent().build();
            case TOO_LARGE -> throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
            case MISSING_FILE -> throw new BadRequestException(MessageKeys.CAR_INSURANCE_INVALID);
            case BUSINESS_ERROR, READ_ERROR -> throw new BadRequestException(
                    outcome.getLocalizedMessage().orElse(MessageKeys.CAR_INSURANCE_INVALID));
        };
    }

    public Response delete(final long carId) {
        final Car car = requireCarExists(carId);
        carService.clearCarInsuranceDocument(car.getOwnerId(), carId);
        return Response.noContent().build();
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }

    private Response sensitiveBinary(final BinaryContent content) {
        final String name = content.getFileName() == null || content.getFileName().isBlank()
                ? BinaryContentResponses.fallbackFileName("insurance", content.getContentType())
                : content.getFileName();
        return CacheableBinaryResponses.sensitive(content, name);
    }

    private static String resolveUploadFileName(final HttpHeaders httpHeaders, final String contentType) {
        final String custom = httpHeaders.getHeaderString("X-Ryden-Filename");
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }
        return BinaryContentResponses.fallbackFileName("insurance", contentType);
    }
}
