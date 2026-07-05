package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.support.BinaryContentResponses;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.facade.CarInsuranceUploadFacade;

/**
 * Car insurance document ({@code /cars/{id}/insurance}).
 *
 * The owner/admin gates are declarative ({@code @PreAuthorize}, backed by the
 * {@code carResourceAccess} bean referenced by name), so it isn't injected as a field.
 */
@Path("/cars/{id}/insurance")
@Component
public class CarInsuranceController {

    private final CarService carService;
    private final StoredFileService storedFileService;
    private final CarInsuranceUploadFacade carInsuranceUploadFacade;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public CarInsuranceController(
            final CarService carService,
            final StoredFileService storedFileService,
            final CarInsuranceUploadFacade carInsuranceUploadFacade,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.storedFileService = storedFileService;
        this.carInsuranceUploadFacade = carInsuranceUploadFacade;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    @GET
    @PreAuthorize("@carResourceAccess.isOwnerOrAdminById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response downloadInsurance(@P("id") @PathParam("id") final long carId) {
        final Car car = requireCarExists(carId);
        final Long fileId = car.getInsuranceFileId().orElse(null);
        if (fileId == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return storedFileService.findContentById(fileId)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadInsurance(@P("id") @PathParam("id") final long carId, final InputStream body)
            throws IOException {
        final Car car = requireCarExists(carId);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final String filename = resolveUploadFileName(contentType);
        final var outcome = carInsuranceUploadFacade.attemptUploadFromBytes(
                car.getOwnerId(), carId, filename, contentType, bytes);
        return switch (outcome.getStatus()) {
            case OK -> Response.noContent().build();
            case TOO_LARGE, BUSINESS_ERROR ->
                    Response.status(Response.Status.BAD_REQUEST)
                            .header("X-Ryden-Error", outcome.getLocalizedMessage().orElse(""))
                            .build();
            default -> Response.status(Response.Status.BAD_REQUEST).build();
        };
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }

    private Response binaryResponse(final BinaryContent content) {
        return BinaryContentResponses.inline(content, "insurance");
    }

    private String resolveUploadFileName(final String contentType) {
        final String custom = httpHeaders.getHeaderString("X-Ryden-Filename");
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }
        return BinaryContentResponses.fallbackFileName("insurance", contentType);
    }
}
