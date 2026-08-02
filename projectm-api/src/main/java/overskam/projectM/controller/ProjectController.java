package overskam.projectM.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.dto.*;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.ProjectService;

import java.util.List;
import java.util.SequencedMap;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    
    @GetMapping
    public ResponseEntity<?> getProjectsList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to get his projects list...", userId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Projects list was fetched successfully",
                        projectService.getProjectsList(userId, page, size, sortBy, sortDirection)
                )
        );
    }
    
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to get his project with id: {}", userId, projectId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Project was fetched successfully",
                        projectService.getProject(userId, projectId)
                )
        );
    }
    
    @PostMapping
    public ResponseEntity<?> createProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid ProjectCreationRequest projectCreationRequest
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to create new project", userId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "New project was created successfully",
                        projectService.createProject(userId, projectCreationRequest.name())
                )
        );
    }
    
    @PatchMapping("/{projectId}/metadata")
    public ResponseEntity<?> updateProjectsMetadata(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody @Valid ProjectMetadataRequest projectMetadataRequest
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to update metadata of project with id: {}", userId, projectId);
        projectService.updateProjectMetadata(userId, projectId, projectMetadataRequest);
        return ResponseEntity.ok(new ApiResponse<>("Projects metadata was updated successfully", null));
    }
    
    @PatchMapping("/{projectId}/data")
    public ResponseEntity<?> updateProjectsData(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody List<SequencedMap<String, Object>> patch
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to update data of project with id: {}", userId, projectId);
        projectService.updateProjectData(userId, projectId, patch);
        return ResponseEntity.ok(new ApiResponse<>("Projects data was updated successfully", null));
    }
    
    @PutMapping("/{projectId}/data")
    public ResponseEntity<?> replaceProjectData(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody @Valid ReplaceProjectDataRequest dataRequest
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to replace project data of project with id: {}", userId, projectId);
        projectService.replaceProjectData(userId, projectId, dataRequest);
        return ResponseEntity.ok(new ApiResponse<>("Projects data was updated successfully", null));
    }
    
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        UUID userId = principal.getUser().getId();
        log.info("User with id: {} is trying to delete his project with id: {}", userId, projectId);
        projectService.deleteProject(userId, projectId);
        return ResponseEntity.ok(new ApiResponse<>("Project was deleted successfully", null));
    }
}
