package overskam.projectM.repository.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import overskam.projectM.AbstractIntegrationTest;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.config.JpaAuditingConfig;
import overskam.projectM.model.PluginBuild;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PluginBuildRepositoryTest extends AbstractIntegrationTest {
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @ParameterizedTest(name = "first build {0}")
    @EnumSource(value = BuildStatus.class, names = {"QUEUED", "RUNNING"})
    @DisplayName("Refuses a second active build for the same project")
    void rejectsSecondActiveBuildForSameProject(BuildStatus firstStatus) {
        UUID ownerId = UUID.randomUUID();
        
        PluginBuild first = new PluginBuild();
        first.setProjectId("abc123");
        first.setOwnerId(ownerId);
        first.setStatus(firstStatus);
        
        PluginBuild second = new PluginBuild();
        second.setProjectId("abc123");
        second.setOwnerId(ownerId);
        
        assertDoesNotThrow(() -> pluginBuildRepository.saveAndFlush(first));
        
        assertThrows(DataIntegrityViolationException.class,
                () -> pluginBuildRepository.saveAndFlush(second));
    }
    
    @ParameterizedTest(name = "first build {0}")
    @EnumSource(value = BuildStatus.class, names = {"SUCCESS", "FAILED"})
    @DisplayName("Allows a new build once the previous one has finished")
    void allowsNewBuildWhenPreviousBuildIsFinished(BuildStatus firstStatus) {
        UUID ownerId = UUID.randomUUID();
        
        PluginBuild first = new PluginBuild();
        first.setProjectId("abc123");
        first.setOwnerId(ownerId);
        first.setStatus(firstStatus);
        
        PluginBuild second = new PluginBuild();
        second.setProjectId("abc123");
        second.setOwnerId(ownerId);
        
        assertDoesNotThrow(() -> pluginBuildRepository.saveAndFlush(first));
        assertDoesNotThrow(() -> pluginBuildRepository.saveAndFlush(second));
    }
}