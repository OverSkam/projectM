package overskam.projectM.worker.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.model.PluginBuild;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PluginBuildRepository extends JpaRepository<PluginBuild, UUID> {
    List<PluginBuild> findByProjectId(String projectId);
    
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PluginBuild b
           SET b.status = :to, b.lastModifiedDate = CURRENT_TIMESTAMP
         WHERE b.id = :id AND b.status = :from
        """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("from") BuildStatus from,
                              @Param("to") BuildStatus to);
    
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PluginBuild b
           SET b.status = :to, b.errorMessage = :reason, b.lastModifiedDate = CURRENT_TIMESTAMP
         WHERE b.status = :from AND b.lastModifiedDate < :threshold
        """)
    int failStale(@Param("from") BuildStatus from,
                  @Param("to") BuildStatus to,
                  @Param("reason") String reason,
                  @Param("threshold") LocalDateTime threshold);
    
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PluginBuild b
           SET b.status = :to,
               b.artifactKey = :artifactKey,
               b.errorMessage = :errorMessage,
               b.lastModifiedDate = CURRENT_TIMESTAMP
         WHERE b.id = :id AND b.status = :from
    """)
    int finishIfCurrent(@Param("id") UUID id,
                        @Param("from") BuildStatus from,
                        @Param("to") BuildStatus to,
                        @Param("artifactKey") String artifactKey,
                        @Param("errorMessage") String errorMessage);
}
