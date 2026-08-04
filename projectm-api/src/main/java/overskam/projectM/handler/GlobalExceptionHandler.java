package overskam.projectM.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.exception.ApiException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        log.warn("[{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(e.getMessage(), null));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleException(DisabledException e) {
        log.warn("Disabled exception [DisabledException]: ", e);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(e.getMessage(), null));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> Objects.requireNonNullElse(err.getDefaultMessage(), "invalid"),
                        (a, b) -> a));
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("Validation failed", errors));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleConflict(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("Resource already exists", null));
    }
}
