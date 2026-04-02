package com.example.miniagentflow.api;

import com.example.miniagentflow.api.dto.ApiErrorResponse;
import com.example.miniagentflow.exception.WorkflowValidationException;
import com.example.miniagentflow.lock.DistributedLockAcquireException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({WorkflowValidationException.class, ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponse.builder()
                        .code("BAD_REQUEST")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(DistributedLockAcquireException.class)
    public ResponseEntity<ApiErrorResponse> handleLockAcquireFailure(DistributedLockAcquireException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(
                ApiErrorResponse.builder()
                        .code("LOCK_ACQUIRE_FAILED")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.builder()
                        .code("INTERNAL_ERROR")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
