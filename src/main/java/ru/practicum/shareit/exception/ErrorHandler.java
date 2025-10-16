package ru.practicum.shareit.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    // --- 400: ошибки валидации данных ---
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorResponse(e.getMessage()));
    }

    // --- 400: ошибки Bean Validation в теле запроса ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации полей запроса: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(errors);
    }

    // --- 400: некорректные параметры запроса (например, query params) ---
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Ошибка валидации параметра: {}", e.getMessage());
        // Берем первое сообщение об ошибке
        String message = e.getConstraintViolations().stream()
                          .findFirst()
                          .map(ConstraintViolation::getMessage)
                          .orElse("Некорректные параметры запроса");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    // --- 404: ресурс не найден ---
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        log.warn("Объект не найден: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body(new ErrorResponse(e.getMessage()));
    }

    // --- 403: нет прав доступа ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Доступ запрещен: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                             .body(new ErrorResponse(e.getMessage()));
    }

    // --- 409: конфликт (например, дублирование email)
    @ExceptionHandler(ValidationExceptionDuplicate.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptionDuplicate(ValidationExceptionDuplicate e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(new ErrorResponse(e.getMessage()));
    }

    // --- 500: все прочие необработанные исключения ---
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Throwable e) {
        log.warn("Непредвиденная ошибка: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Произошла непредвиденная ошибка: " + e.getMessage()));
    }
}
