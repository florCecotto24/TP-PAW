package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.PictureDto;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CarGalleryUploadSupport;

/**
 * Car gallery sub-resource ({@code /cars/{id}/pictures}).
 *
 * <p>The owner gate is declarative ({@code @PreAuthorize}, backed by the {@code carResourceAccess}
 * bean referenced by name), so it isn't injected as a field.
 */
@Path("/cars/{id}/pictures")
@Component
public class CarPictureController {

    private final CarService carService;
    private final CarPictureService carPictureService;
    private final ImageService imageService;
    private final StoredFileService storedFileService;
    private final CarGalleryUploadSupport carGalleryUploadSupport;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    @Autowired
    public CarPictureController(
            final CarService carService,
            final CarPictureService carPictureService,
            final ImageService imageService,
            final StoredFileService storedFileService,
            final CarGalleryUploadSupport carGalleryUploadSupport,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.imageService = imageService;
        this.storedFileService = storedFileService;
        this.carGalleryUploadSupport = carGalleryUploadSupport;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    @GET
    @Produces(VndMediaType.PICTURE_V1_JSON)
    public Response listPictures(@PathParam("id") final long carId) {
        requireCarExists(carId);
        final List<PictureDto> dtos = carPictureService.getCarPicturesByCarId(carId).stream()
                .map(picture -> PictureDto.from(picture, uriInfo))
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(dtos).header("X-Total-Count", dtos.size()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.PICTURE_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response addPicture(
            @P("id") @PathParam("id") final long carId,
            @FormDataParam("file") final InputStream fileBody,
            @FormDataParam("file") final org.glassfish.jersey.media.multipart.FormDataContentDisposition fileMeta,
            @FormDataParam("displayOrder") final Integer displayOrder) throws IOException {
        final Car car = requireCarExists(carId);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(fileBody);
        final String filename = fileMeta != null ? fileMeta.getFileName() : "upload";
        final String contentType = fileMeta != null && fileMeta.getType() != null
                ? fileMeta.getType()
                : MediaType.APPLICATION_OCTET_STREAM;
        final int order = displayOrder != null && displayOrder > 0
                ? displayOrder
                : carGalleryUploadSupport.nextDisplayOrder(carId);
        carGalleryUploadSupport.attachGalleryMedia(
                car.getOwnerId(),
                carId,
                List.of(new GalleryMediaUpload(filename, contentType, bytes)),
                order);
        final List<CarPicture> pictures = carPictureService.getCarPicturesByCarId(carId);
        final CarPicture created = pictures.get(pictures.size() - 1);
        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("pictures").path(String.valueOf(created.getId()))
                .build();
        return Response.created(location).build();
    }

    @GET
    @Path("/{pictureId}")
    public Response getPictureBytes(
            @PathParam("id") final long carId,
            @PathParam("pictureId") final long pictureId) {
        requireCarExists(carId);
        final CarPicture picture = carPictureService.getCarPictureById(pictureId)
                .filter(p -> p.getCarId() == carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (picture.isVideo()) {
            final Long storedFileId = picture.getStoredFileId();
            if (storedFileId == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return storedFileService.findContentById(storedFileId)
                    .map(content -> CacheableBinaryResponses.of(request, content))
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
        }
        final Long imageId = picture.getImageId();
        if (imageId == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return imageService.getImageContent(imageId)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{pictureId}")
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deletePicture(
            @P("id") @PathParam("id") final long carId,
            @PathParam("pictureId") final long pictureId) {
        requireCarExists(carId);
        carPictureService.deleteCarPictureForCar(carId, pictureId);
        return Response.noContent().build();
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }
}
