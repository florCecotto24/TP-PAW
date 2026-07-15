package ar.edu.itba.paw.webapp.controller.car;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.PictureDto;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.CarGalleryUploadSupport;
import ar.edu.itba.paw.webapp.support.CarResourceAccess;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;

/**
 * Car gallery sub-resource ({@code /cars/{id}/pictures}).
 *
 * Gallery metadata ({@code list}) is gated by {@link CarResourceAccess#canViewCar}; raw
 * picture bytes ({@code /primary}, {@code /{pictureId}}) stay anonymously readable so
 * {@code <img src>} thumbnails work without an {@code Authorization} header (browse +
 * owner cards). Car JSON detail remains protected separately on {@code GET /cars/{id}}.
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
    private final PaginationSupport paginationSupport;
    private final CurrentUserResolver currentUserResolver;
    private final CarResourceAccess carResourceAccess;

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
            final BinaryPayloadSupport binaryPayloadSupport,
            final PaginationSupport paginationSupport,
            final CurrentUserResolver currentUserResolver,
            final CarResourceAccess carResourceAccess) {
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.imageService = imageService;
        this.storedFileService = storedFileService;
        this.carGalleryUploadSupport = carGalleryUploadSupport;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.paginationSupport = paginationSupport;
        this.currentUserResolver = currentUserResolver;
        this.carResourceAccess = carResourceAccess;
    }

    @GET
    @Produces(VndMediaType.PICTURE_V1_JSON)
    public Response listPictures(
            @PathParam("id") final long carId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam) {
        carResourceAccess.requireViewableCar(carId, currentUserResolver.currentPrincipalOrNull());
        final PaginationParams paging = paginationSupport.forCarGallery(page, pageSizeParam);
        // Metadata-only projection: the gallery list needs id/order/content-type, not the blobs.
        final Page<CarPictureSummary> picturePage = carPictureService.findSummariesByCarPaginated(
                carId, paging.getZeroBasedPage(), paging.getPageSize());
        final List<PictureDto> dtos = picturePage.getContent().stream()
                .map(summary -> PictureDto.from(summary, uriInfo))
                .collect(Collectors.toList());
        final int totalItems = (int) picturePage.getTotalItems();
        if (totalItems == 0L || dtos.isEmpty()) {
            return Response.noContent().build();
        }
        final Response.ResponseBuilder builder =
                Response.ok(new GenericEntity<List<PictureDto>>(dtos) {})
                        .header("X-Total-Count", totalItems);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), totalItems);
        return builder.build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.PICTURE_V1_JSON)
    @PreAuthorize("@carResourceAccess.isOwnerById(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response addPicture(
            @P("id") @PathParam("id") final long carId,
            @FormDataParam("file") final FormDataBodyPart filePart) throws IOException {
        final Car car = requireCarExists(carId);
        if (filePart == null) {
            throw new CarValidationException(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE);
        }
        final byte[] bytes = filePart.getEntityAs(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new CarValidationException(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE);
        }
        final String filename = filePart.getContentDisposition() != null
                ? filePart.getContentDisposition().getFileName()
                : "upload";
        final String contentType = filePart.getMediaType() != null
                ? filePart.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final int order = carGalleryUploadSupport.nextDisplayOrder(carId);
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
    @Path("/primary")
    public Response getPrimaryPictureBytes(@PathParam("id") final long carId) {
        requireCarExists(carId);
        final CarPicture picture = carPictureService.findPrimaryPictureByCarId(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return pictureBytesResponse(carId, picture);
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
        return pictureBytesResponse(carId, picture);
    }

    private Response pictureBytesResponse(final long carId, final CarPicture picture) {
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
