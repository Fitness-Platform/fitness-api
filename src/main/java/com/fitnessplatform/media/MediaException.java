package com.fitnessplatform.media;

import org.springframework.http.HttpStatus;

public class MediaException extends RuntimeException {
    public enum Reason {
        UNSUPPORTED_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type for this purpose."),
        INVALID_SIZE(HttpStatus.BAD_REQUEST, "File size must be greater than zero."),
        TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the allowed size for this purpose."),
        INVALID_REFERENCE(HttpStatus.BAD_REQUEST, "Invalid media upload reference."),
        NOT_COMPLETED(HttpStatus.CONFLICT, "Upload must be completed and verified before use."),
        EXPIRED(HttpStatus.CONFLICT, "Upload authorization has expired. Request a new upload."),
        MISSING(HttpStatus.NOT_FOUND, "Uploaded object was not found. Upload the file before completing."),
        VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "Uploaded file does not match the authorized metadata."),
        SOURCE_CONFLICT(HttpStatus.BAD_REQUEST, "Choose either an external URL or uploaded media."),
        SOURCE_REQUIRED(HttpStatus.BAD_REQUEST, "A resource requires an external URL or verified uploaded media."),
        UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Media storage is temporarily unavailable.");

        private final HttpStatus status;
        private final String detail;
        Reason(HttpStatus status, String detail) { this.status = status; this.detail = detail; }
        public HttpStatus status() { return status; }
    }
    private final Reason reason;
    public MediaException(Reason reason) { super(reason.detail); this.reason = reason; }
    public Reason reason() { return reason; }
}
