package ar.edu.itba.paw.webapp.dto.rest;

import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * REST chat message representation ({@code application/vnd.paw.message.v1+json}).
 */
public final class MessageDto {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String body;
    private String createdAt;
    private boolean seen;
    private boolean hasAttachment;
    private MessageAttachmentDto attachment;
    private LinksDto links;

    public MessageDto() {
    }

    public static MessageDto fromDto(final ReservationMessageDto dto, final UriInfo uriInfo) {
        final MessageDto out = new MessageDto();
        out.body = dto.getBody();
        out.createdAt = dto.getCreatedAt() == null ? null : ISO_OFFSET.format(dto.getCreatedAt());
        out.seen = dto.isSeen();
        out.hasAttachment = dto.getAttachment() != null;
        out.attachment = MessageAttachmentDto.fromInternal(dto.getAttachment());
        out.links = LinksDto.ofSelf(
                        RestUriUtils.reservationMessageUri(uriInfo, dto.getReservationId(), dto.getId()).toString())
                .withRelated("reservation",
                        RestUriUtils.reservationUri(uriInfo, dto.getReservationId()).toString())
                .withRelated("sender", RestUriUtils.userUri(uriInfo, dto.getSenderUserId()).toString());
        if (out.hasAttachment) {
            out.links = out.links.withRelated(
                    "attachment",
                    RestUriUtils.reservationMessageAttachmentUri(
                            uriInfo, dto.getReservationId(), dto.getId()).toString());
        }
        return out;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(final boolean seen) {
        this.seen = seen;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(final boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public MessageAttachmentDto getAttachment() {
        return attachment;
    }

    public void setAttachment(final MessageAttachmentDto attachment) {
        this.attachment = attachment;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
