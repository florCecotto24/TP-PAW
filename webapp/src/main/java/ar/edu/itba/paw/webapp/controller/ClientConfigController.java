package ar.edu.itba.paw.webapp.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.policy.ChatAttachmentUploadPolicy;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppMoneyProperties;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;
import ar.edu.itba.paw.webapp.dto.rest.ClientConfigDto;

/** Public SPA limits ({@code GET /api/config}, {@code openapi.yaml} {@code ClientConfig}). */
@Path("/config")
@Component
public final class ClientConfigController {

    private final AppValidationProperties appValidationProperties;
    private final AppMoneyProperties appMoneyProperties;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final CarGalleryUploadPolicy carGalleryUploadPolicy;
    private final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy;
    private final Environment environment;

    @Autowired
    public ClientConfigController(
            final AppValidationProperties appValidationProperties,
            final AppMoneyProperties appMoneyProperties,
            final ReservationTimingPolicy reservationTimingPolicy,
            final CarGalleryUploadPolicy carGalleryUploadPolicy,
            final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy,
            final Environment environment) {
        this.appValidationProperties = appValidationProperties;
        this.appMoneyProperties = appMoneyProperties;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.carGalleryUploadPolicy = carGalleryUploadPolicy;
        this.chatAttachmentUploadPolicy = chatAttachmentUploadPolicy;
        this.environment = environment;
    }

    @GET
    @Produces(VndMediaType.CLIENT_CONFIG_V1_JSON)
    public Response config() {
        return Response.ok(ClientConfigDto.from(
                appValidationProperties,
                appMoneyProperties,
                reservationTimingPolicy,
                carGalleryUploadPolicy,
                chatAttachmentUploadPolicy,
                environment)).build();
    }
}
