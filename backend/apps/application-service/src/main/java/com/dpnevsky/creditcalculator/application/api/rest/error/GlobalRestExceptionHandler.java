package com.dpnevsky.creditcalculator.application.api.rest.error;

import com.dpnevsky.creditcalculator.application.application.exception.ApplicationAccessDeniedException;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationDocumentNotFoundException;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.application.exception.ContractSigningConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
        List<Map<String, String>> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "VALIDATION_ERROR");
        body.put("message", "Ошибка валидации запроса");
        body.put("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationNotFound(ApplicationNotFoundException exception) {
        return toNotFoundResponse(exception.getMessage());
    }

    @ExceptionHandler(ApplicationDocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationDocumentNotFound(
            ApplicationDocumentNotFoundException exception
    ) {
        return toNotFoundResponse(exception.getMessage());
    }

    @ExceptionHandler(ApplicationAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationAccessDenied(
            ApplicationAccessDeniedException exception
    ) {
        return toErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(ContractSigningConflictException.class)
    public ResponseEntity<Map<String, Object>> handleContractSigningConflict(
            ContractSigningConflictException exception
    ) {
        return toErrorResponse(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> toNotFoundResponse(String message) {
        return toErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private ResponseEntity<Map<String, Object>> toErrorResponse(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", errorCode);
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "BUSINESS_ERROR");
        body.put("message", exception.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "INTERNAL_ERROR");
        body.put("message", "Непредвиденная ошибка сервера");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, String> toValidationError(FieldError fieldError) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        return error;
    }
}
