package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.BuildService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class BuildController {
    private final BuildService buildService;
    
    @PostMapping("/{projectId}/compile")
    public ResponseEntity<?> compile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        User user = principal.getUser();
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Compile task was queued",
                        Map.of("buildId", buildService.buildProject(user, projectId))
                )
        );
    }
}
