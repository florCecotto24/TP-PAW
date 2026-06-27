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
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.facade.CarInsuranceUploadFacade;

/** Car insurance document ({@code /cars/{id}/insurance}). */
@Path("/cars/{id}/insurance")
@Component
public final class CarInsuranceController {

    private final CarService carService;
    private final StoredFileService storedFileService;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;
    private final CarInsuranceUploadFacade carInsuranceUploadFacade;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public CarInsuranceController(
            final CarService carService,
            final StoredFileService storedFileService,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess,
            final CarInsuranceUploadFacade carInsuranceUploadFacade,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.storedFileService = storedFileService;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
        this.carInsuranceUploadFacade = carInsuranceUploadFacade;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    @GET
    public Response downloadInsurance(@PathParam("id") final long carId) {
        final Car car = requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        carResourceAccess.requireOwnerOrAdmin(car, viewer);
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
    public Response uploadInsurance(@PathParam("id") final long carId, final InputStream body)
            throws IOException {
        final Car car = requireCarExists(carId);
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        carResourceAccess.requireOwner(car, viewer);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final var outcome = carInsuranceUploadFacade.attemptUploadFromBytes(
                car.getOwnerId(), carId, "insurance", contentType, bytes);
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
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .build();
    }
}
