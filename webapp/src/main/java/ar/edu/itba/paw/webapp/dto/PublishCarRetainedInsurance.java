package ar.edu.itba.paw.webapp.dto;

import java.io.Serializable;

/**
 * Metadata of a retained insurance document between POST with errors. The binary content lives on disk
 * (temporary on the server), keyed by session id; only this metadata is serialized into the HTTP session.
 * Unlike {@link PublishCarRetainedImage} there is at most one stashed insurance per session, so no token
 * is needed.
 */
public record PublishCarRetainedInsurance(String filename, String contentType, long sizeBytes) implements Serializable {

    private static final long serialVersionUID = 1L;

    public PublishCarRetainedInsurance {
        filename = filename != null ? filename : "";
        contentType = contentType != null ? contentType : "";
        if (sizeBytes < 0L) {
            sizeBytes = 0L;
        }
    }
}
