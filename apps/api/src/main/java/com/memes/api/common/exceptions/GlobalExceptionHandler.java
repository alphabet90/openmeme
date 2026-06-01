package com.memes.api.common.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(Map.of(
            "status", 400,
            "error", "Validation Failed",
            "errors", errors
        ));
    }

    @ExceptionHandler(MemeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMemeNotFound(MemeNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidApiKey(InvalidApiKeyException ex) {
        return ResponseEntity.status(401).body(Map.of(
            "status", 401,
            "error", "Unauthorized",
            "message", ex.getMessage()
        ));
    }
}
