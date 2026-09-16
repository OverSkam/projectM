package overskam.projectM.repository.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import overskam.projectM.config.JpaAuditingConfig;
import overskam.projectM.model.PluginBuild;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@Import(JpaAuditingConfig.class)
class PluginBuildRepositoryTest {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @Test
    @DisplayName("Refuses a second active build for the same project")
    void rejectsSecondActiveBuildForSameProject() {
        UUID ownerId = UUID.randomUUID();
        
        PluginBuild first = new PluginBuild();
        first.setProjectId("abc123");
        first.setOwnerId(ownerId);
        
        PluginBuild second = new PluginBuild();
        second.setProjectId("abc123");
        second.setOwnerId(ownerId);
        
        assertDoesNotThrow(() -> pluginBuildRepository.saveAndFlush(first));
        
        assertThrows(DataIntegrityViolationException.class,
                () -> pluginBuildRepository.saveAndFlush(second));
    }
}