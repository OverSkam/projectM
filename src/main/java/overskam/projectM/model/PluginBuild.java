package overskam.projectM.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import overskam.projectM.exception.BuildStatus;

import java.util.UUID;

@Data
@Entity
@Table(name = "plugin_builds")
@EqualsAndHashCode(callSuper = true)
public class PluginBuild extends AbstractModel {
    @Column(name = "project_id", nullable = false)
    private String projectId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BuildStatus status = BuildStatus.QUEUED;
    
    @Column(name = "artifact_key")
    private String artifactKey;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
