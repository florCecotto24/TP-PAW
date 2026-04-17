package ar.edu.itba.paw.webapp.dto;

import java.io.Serializable;

/**
 * Metadata of a retained picture between POST with errors: the binary content lives on disk (temporary on the server).
 */
public record PublishCarRetainedImage(String stashToken, String filename, String contentType) implements Serializable {

    private static final long serialVersionUID = 2L;

    public PublishCarRetainedImage {
        stashToken = stashToken != null ? stashToken : "";
        filename = filename != null ? filename : "";
        contentType = contentType != null ? contentType : "";
    }
}
