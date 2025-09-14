package org.apache.coyote.http11;

public enum HttpStatus {

    OK(200),

    FOUND(302),

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    CONFLICT(409);

    private final int statusCode;

    HttpStatus(final int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
