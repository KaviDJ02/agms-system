package com.agms.zone_service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Feign call to external IoT API failed
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        log.error("IoT API call failed [status={}]: {}", ex.status(), ex.getMessage());
        String message = switch (ex.status()) {
            case 401 -> "IoT API authentication failed — check credentials";
            case 404 -> "IoT API resource not found — check base-url or endpoint";
            case 409 -> "IoT API conflict — user or device may already exist";
            default  -> "IoT API error (status " + ex.status() + "): " + ex.getMessage();
        };
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "IoT API Error",
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }

    // @Valid validation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Validation Failed",
                "message", errors,
                "timestamp", Instant.now().toString()
        ));
    }

    // Zone not found
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
