package overskam.projectM.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.exception.ApiException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        
        if (body == null || body instanceof ProblemDetail) {
            String message = e instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null
                    ? errorResponse.getBody().getDetail()
                    : HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
            body = new ApiResponse<>(message, null);
        }
        return super.handleExceptionInternal(e, body, headers, statusCode, request);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        log.warn("[{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(e.getMessage(), null));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException e) {
        log.warn("Disabled exception [DisabledException]: ", e);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(e.getMessage(), null));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("Resource already exists", null));
    }
    
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> Objects.requireNonNullElse(err.getDefaultMessage(), "invalid"),
                        (a, b) -> a));
        
        return handleExceptionInternal(ex, new ApiResponse<>("Validation failed", errors), headers, status, request);
    }
    
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        
        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage());
        return handleExceptionInternal(ex, new ApiResponse<>("Malformed request body", null), headers, status, request);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("Invalid parameter", Map.of(e.getName(), "invalid value")));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("Invalid credentials", null));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleUnexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled exception, traceId={}", traceId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("Internal error", Map.of("traceId", traceId)));
    }
}
