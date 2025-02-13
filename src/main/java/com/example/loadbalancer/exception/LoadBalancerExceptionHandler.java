package com.example.loadbalancer.exception;

import com.example.loadbalancer.model.ErrorResponse;
import com.example.loadbalancer.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for handling application-wide exceptions
 * in a centralized manner.
 */
@Slf4j
@RestControllerAdvice
public class LoadBalancerExceptionHandler {

    @ExceptionHandler(NoAvailableInstanceException.class)
    public ResponseEntity<ErrorResponse> handleNoAvailableInstanceException(
            NoAvailableInstanceException exception) {
        log.error("No Available Instances to handle the request {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(Constants.ERROR_NO_AVAILABLE_INSTANCE, exception.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(InvalidRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(Constants.ERROR_INVALID_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(Constants.ERROR_INTERNAL_SERVER,"An unexpected error occurred"));
    }
}
