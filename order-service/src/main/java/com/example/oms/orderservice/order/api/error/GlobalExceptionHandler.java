package com.example.oms.orderservice.order.api.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        List<FieldValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ValidationErrorResponse response =
                new ValidationErrorResponse("Validation failed", errors);

        return ResponseEntity.badRequest().body(response);
    }
}
