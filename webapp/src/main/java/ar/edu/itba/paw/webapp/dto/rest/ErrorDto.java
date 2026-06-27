package ar.edu.itba.paw.webapp.dto.rest;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;

/**
 * Error body ({@code openapi.yaml} {@code ErrorDto}).
 */
public final class ErrorDto {

    private int status;
    private String code;
    private String message;

    public ErrorDto() {
    }

    public ErrorDto(final int status, final String code, final String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static String mediaType() {
        return VndMediaType.ERROR_V1_JSON;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}
