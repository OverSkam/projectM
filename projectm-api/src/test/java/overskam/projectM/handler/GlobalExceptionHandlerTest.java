package overskam.projectM.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.exception.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionUsesExceptionStatusAndMessage() {
        ResponseEntity<?> response = handler.handleApiException(new InvalidRequestException("bad request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiResponse<?>) response.getBody()).message()).isEqualTo("bad request");
    }

    @Test
    void handleDisabledExceptionReturnsForbidden() {
        ResponseEntity<?> response = handler.handleDisabled(new DisabledException("disabled"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ApiResponse<?>) response.getBody()).message()).isEqualTo("disabled");
    }
}
