package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.CarPicture;
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
