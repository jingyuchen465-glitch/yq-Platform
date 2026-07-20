package com.itcjy.common.exception;

import com.itcjy.common.pojo.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.error("Business exception: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = buildValidationMessage(ex.getBindingResult().getFieldErrors(), "Validation failed");
        log.error("Method argument validation failed: {}", message, ex);
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String message = buildValidationMessage(ex.getBindingResult().getFieldErrors(), "Invalid request parameters");
        log.error("Bind exception: {}", message, ex);
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage(), ex);
        return ApiResponse.fail(400, "Validation failed");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Message not readable", ex);
        return ApiResponse.fail(400, "Failed to parse request body");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("System exception", ex);
        return ApiResponse.fail(500, "System is busy, please try again later");
    }

    private String buildValidationMessage(List<FieldError> fieldErrors, String defaultMessage) {
        String message = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));
        return message.isBlank() ? defaultMessage : message;
    }
}
