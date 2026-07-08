package overskam.projectM.controller;

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

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    
    @GetMapping
    public ResponseEntity<?> getProjectsList(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        log.info("User is trying to get his projects list...");
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Projects list was fetched successfully",
                        projectService.getProjectsList(user)
                )
        );
    }
    
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        User user = principal.getUser();
        log.info("User is trying to get his project with id: {}", projectId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Project was fetched successfully",
                        projectService.getProject(user, projectId)
                )
        );
    }
    
    @PostMapping
    public ResponseEntity<?> createProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ProjectCreationRequest projectCreationRequest
    ) {
        User user = principal.getUser();
        log.info("User is trying to create new project");
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "New project was created successfully",
                        projectService.createProject(user, projectCreationRequest.name())
                )
        );
    }
    
    @PatchMapping("/{projectId}/metadata")
    public ResponseEntity<?> updateProjectsMetadata(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody ProjectMetadataRequest projectMetadataRequest
    ) {
        User user = principal.getUser();
        log.info("User is trying to update metadata of project with id: {}", projectId);
        projectService.updateProjectMetadata(user, projectId, projectMetadataRequest);
        return ResponseEntity.ok(new ApiResponse<>("Projects metadata was updated successfully", null));
    }
    
    @PatchMapping("/{projectId}/data")
    public ResponseEntity<?> updateProjectsData(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody List<SequencedMap<String, Object>> patch
    ) {
        User user = principal.getUser();
        log.info("User is trying to update data of project with id: {}", projectId);
        projectService.updateProjectData(user, projectId, patch);
        return ResponseEntity.ok(new ApiResponse<>("Projects data was updated successfully", null));
    }
    
    @PutMapping("/{projectId}/data")
    public ResponseEntity<?> replaceProjectData(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId,
            @RequestBody ReplaceProjectDataRequest dataRequest
    ) {
        User user = principal.getUser();
        log.info("User is trying to replace project data of project with id: {}", projectId);
        projectService.replaceProjectData(user, projectId, dataRequest);
        return ResponseEntity.ok(new ApiResponse<>("Projects data was updated successfully", null));
    }
    
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String projectId
    ) {
        User user = principal.getUser();
        log.info("User is trying to delete his project with id: {}", projectId);
        projectService.deleteProject(user, projectId);
        return ResponseEntity.ok(new ApiResponse<>("Project was deleted successfully", null));
    }
}
