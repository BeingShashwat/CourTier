package com.courtier.courtier.common.exception;

import org.springframework.http.HttpStatus;

public abstract class CourtierException extends RuntimeException {
    public CourtierException(String message) {
        super(message);
    }

    public abstract HttpStatus status();

    public static class NotFound extends CourtierException {
        public NotFound(String message) {
            super(message);
        }

        @Override
        public HttpStatus status() {
            return HttpStatus.NOT_FOUND;
        }
    }

    public static class Conflict extends CourtierException {
        public Conflict(String message) {
            super(message);
        }

        @Override
        public HttpStatus status() {
            return HttpStatus.CONFLICT;
        }
    }

    public static class BadRequest extends CourtierException {
        public BadRequest(String message) {
            super(message);
        }

        @Override
        public HttpStatus status() {
            return HttpStatus.BAD_REQUEST;
        }
    }

    public static class Unauthorized extends CourtierException {
        public Unauthorized(String message) {
            super(message);
        }

        @Override
        public HttpStatus status() {
            return HttpStatus.UNAUTHORIZED;
        }
    }

    public static class Forbidden extends CourtierException {
        public Forbidden(String message) {
            super(message);
        }

        @Override
        public HttpStatus status() {
            return HttpStatus.FORBIDDEN;
        }
    }
}
