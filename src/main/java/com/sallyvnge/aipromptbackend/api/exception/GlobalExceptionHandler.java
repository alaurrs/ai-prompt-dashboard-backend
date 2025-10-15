package com.sallyvnge.aipromptbackend.api.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> onValidation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> onType(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body("Bad parameter: " + e.getName());
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> onOptimisticLock(OptimisticLockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Version mismatch");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> onIllegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
