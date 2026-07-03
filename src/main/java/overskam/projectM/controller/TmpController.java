package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class TmpController {
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<?> tmpGet(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        log.info("So far so good...");
        return ResponseEntity.ok(new ApiResponse<>("So far so good with user: ", user.getId()));
    }
}
