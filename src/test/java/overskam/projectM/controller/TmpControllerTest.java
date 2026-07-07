package overskam.projectM.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.UserService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TmpControllerTest {

    @Test
    void tmpGetReturnsAuthenticatedUserId() {
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        TmpController controller = new TmpController(mock(UserService.class));

        ResponseEntity<?> response = controller.tmpGet(new CustomUserDetails(user));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("So far so good with user: ");
        assertThat(body.data()).isEqualTo(userId);
    }
}
