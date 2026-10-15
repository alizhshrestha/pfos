package com.pfos.backend.dto;

public record ApiErrorResponse(
        boolean success,
        ErrorDetail error
) {
    public record ErrorDetail(String code, String message) {
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(
                false,
                new ErrorDetail(code, message)
        );
    }
}