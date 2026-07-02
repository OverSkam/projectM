package overskam.projectM.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.exception.ApiException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        log.warn("[{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(e.getMessage(), null));
    }
}
