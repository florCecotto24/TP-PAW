package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.dto.rest.PictureDto;

/**
 * HTTP binding for car gallery metadata, multipart upload, and binary bytes.
 */
@Component
public final class CarPictureHttpSupport {

    private final CarService carService;
    private final CarPictureService carPictureService;
    private final ImageService imageService;
    private final StoredFileService storedFileService;
    private final CarGalleryUploadSupport carGalleryUploadSupport;
    private final BinaryPayloadSupport binaryPayloadSupport;

    public CarPictureHttpSupport(
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

    public Response list(final long carId, final PaginationParams paging, final UriInfo uriInfo) {
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

    public Response add(
            final long carId,
            final FormDataBodyPart filePart,
            final UriInfo uriInfo) throws IOException {
        final Car car = requireCarExists(carId);
        final InputStream stream = filePart == null ? null : filePart.getEntityAs(InputStream.class);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(stream);
        final String filename = filePart.getContentDisposition() != null
                ? filePart.getContentDisposition().getFileName()
                : "upload";
        final String contentType = filePart.getMediaType() != null
                ? filePart.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final int order = carGalleryUploadSupport.nextDisplayOrder(carId);
        final CarPicture created = carGalleryUploadSupport.attachSingleGalleryMedia(
                car.getOwnerId(),
                carId,
                new GalleryMediaUpload(filename, contentType, bytes),
                order);
        final URI location = uriInfo.getBaseUriBuilder()
                .path("cars").path(String.valueOf(carId))
                .path("pictures").path(String.valueOf(created.getId()))
                .build();
        return Response.created(location)
                .entity(PictureDto.from(created, uriInfo))
                .build();
    }

    public Response primaryBytes(final long carId, final Request request) {
        final CarPicture picture = carPictureService.findPrimaryPictureByCarId(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return pictureBytesResponse(picture, request);
    }

    public Response pictureBytes(final long carId, final long pictureId, final Request request) {
        final CarPicture picture = carPictureService.getCarPictureById(pictureId)
                .filter(p -> p.getCarId() == carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        return pictureBytesResponse(picture, request);
    }

    public Response delete(final long carId, final long pictureId) {
        requireCarExists(carId);
        carPictureService.deleteCarPictureForCar(carId, pictureId);
        return Response.noContent().build();
    }

    private Response pictureBytesResponse(final CarPicture picture, final Request request) {
        if (picture.isVideo()) {
            final Long storedFileId = picture.getStoredFileId();
            if (storedFileId == null) {
                throw new NotFoundException();
            }
            return storedFileService.findContentById(storedFileId)
                    .map(content -> CacheableBinaryResponses.of(request, content))
                    .orElseThrow(NotFoundException::new);
        }
        final Long imageId = picture.getImageId();
        if (imageId == null) {
            throw new NotFoundException();
        }
        return imageService.getImageContent(imageId)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseThrow(NotFoundException::new);
    }

    private Car requireCarExists(final long carId) {
        return carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
    }
}
