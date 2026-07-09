package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.BuildService;
import overskam.projectM.service.ProjectService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class BuildController {
    private final BuildService buildService;
    
    @GetMapping("/{projectId}/builds")
    public ResponseEntity<?> getBuilds(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "status") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        User user = principal.getUser();
        log.info("User is trying to get all his builds");
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Project builds was fetched successfully",
                        buildService.getBuilds(user, projectId, page, size, sortBy, sortDirection)
                )
        );
    }
    
    @GetMapping("/{projectId}/builds/{buildId}")
    public ResponseEntity<?> getBuild(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @PathVariable UUID buildId
    ) {
        User user = principal.getUser();
        log.info("User is trying to get build with id: {}", buildId);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Build was fetched successfully",
                        buildService.getBuild(user, projectId, buildId)
                )
        );
    }
    
    @PostMapping("/{projectId}/compile")
    public ResponseEntity<?> compile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        User user = principal.getUser();
        log.info("User is trying to compile project with id: {}", projectId);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Compile task was queued",
                        Map.of("buildId", buildService.buildProject(user, projectId))
                )
        );
    }
}
