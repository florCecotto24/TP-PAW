package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Gallery item metadata ({@code application/vnd.paw.picture.v1+json}). */
public final class PictureDto {

    private int displayOrder;
    private String kind;
    private String contentType;
    private LinksDto links;

    public PictureDto() {
    }

    public static PictureDto from(final CarPicture picture, final UriInfo uriInfo) {
        final PictureDto dto = new PictureDto();
        dto.displayOrder = picture.getDisplayOrder();
        final long carId = picture.getCarId();
        if (picture.isVideo()) {
            dto.kind = "video";
            dto.contentType = picture.getStoredFile() != null
                    ? picture.getStoredFile().getContentType()
                    : "video/mp4";
        } else {
            dto.kind = "image";
            dto.contentType = picture.getImage() != null
                    ? picture.getImage().getContentType()
                    : "image/jpeg";
        }
        dto.links = LinksDto.ofSelf(
                        RestUriUtils.carPictureUri(uriInfo, carId, picture.getId()).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, carId).toString());
        return dto;
    }

    /** Same representation as {@link #from(CarPicture, UriInfo)} but from the blob-free list projection. */
    public static PictureDto from(final CarPictureSummary summary, final UriInfo uriInfo) {
        final PictureDto dto = new PictureDto();
        dto.displayOrder = summary.getDisplayOrder();
        final long carId = summary.getCarId();
        if (summary.isVideo()) {
            dto.kind = "video";
            dto.contentType = summary.getStoredFileContentType() != null
                    ? summary.getStoredFileContentType()
                    : "video/mp4";
        } else {
            dto.kind = "image";
            dto.contentType = summary.getImageContentType() != null
                    ? summary.getImageContentType()
                    : "image/jpeg";
        }
        dto.links = LinksDto.ofSelf(
                        RestUriUtils.carPictureUri(uriInfo, carId, summary.getId()).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, carId).toString());
        return dto;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
