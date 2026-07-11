package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.PluginBuildService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class BuildController {
    private final PluginBuildService pluginBuildService;
    
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
                        pluginBuildService.getBuilds(user, projectId, page, size, sortBy, sortDirection)
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
                        pluginBuildService.getBuild(user, projectId, buildId)
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
                        Map.of("buildId", pluginBuildService.buildProject(user, projectId))
                )
        );
    }
    
    @GetMapping("/{projectId}/builds/{buildId}/artifact")
    public ResponseEntity<?> getArtifact(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @PathVariable UUID buildId
    ) {
        User user = principal.getUser();
        log.info("User is trying to get an artifact link from build with id: {}", buildId);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Artifact link was fetched successfully",
                        pluginBuildService.getArtifact(user, projectId, buildId)
                )
        );
    }
}
