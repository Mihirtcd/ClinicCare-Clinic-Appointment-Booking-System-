package com.tus.cliniccare.exception;

import com.tus.cliniccare.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request body.",
                request.getRequestURI(),
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request parameters.",
                request.getRequestURI(),
                validationErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed request payload.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method is not allowed for this endpoint.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(path);
        if (validationErrors != null) {
            response.setValidationErrors(validationErrors);
        }
        return ResponseEntity.status(status).body(response);
    }
}
