package com.courtier.courtier.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String error) {

    public static <T>ApiResponse<T> ok(T data){
        return new ApiResponse<T>(true, data, null);
    }

    public static <T>ApiResponse<T> fail(String message){
        return new ApiResponse<T>(false, null, message);
    }
}


//Why record?
// Immutable, no boilerplate, perfect for a response wrapper that never changes after construction.