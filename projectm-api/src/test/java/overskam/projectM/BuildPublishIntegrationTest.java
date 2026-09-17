package overskam.projectM;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import overskam.projectM.common.dto.CompileTaskMessage;
import overskam.projectM.dto.BuildQueuedResponse;
import overskam.projectM.model.Project;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.service.BuildQueuePublisher;
import overskam.projectM.service.PluginBuildService;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtb25seS0zMi1ieXRlcw")
class BuildPublishIntegrationTest extends AbstractIntegrationTest {
    
    @MockitoBean
    private BuildQueuePublisher buildQueuePublisher;
    
    @Autowired
    private PluginBuildService pluginBuildService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private PluginBuildRepository buildRepository;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Test
    @DisplayName( "Sends the compile message after the save is committed")
    void sendsMessageAfterSave() {
        UUID ownerId = UUID.randomUUID();
        Project project = new Project();
        project.setOwnerId(ownerId);
        project.setName(ownerId.toString());
        String projectId = projectRepository.save(project).getId();
        
        BuildQueuedResponse response = pluginBuildService.buildProject(ownerId, projectId);
        
        verify(buildQueuePublisher).publish(new CompileTaskMessage(response.buildId(), projectId, ownerId));
    }
    
    @Test
    @DisplayName("Sends nothing and saves nothing when the transaction is undone")
    void sendsNothingWhenTransactionRollsBack() {
        UUID ownerId = UUID.randomUUID();
        Project project = new Project();
        project.setOwnerId(ownerId);
        project.setName(ownerId.toString());
        String projectId = projectRepository.save(project).getId();
        
        transactionTemplate.execute(status -> {
            pluginBuildService.buildProject(ownerId, projectId);
            
            verifyNoInteractions(buildQueuePublisher);
            
            status.setRollbackOnly();
            return null;
        });
        
        verifyNoInteractions(buildQueuePublisher);
    }
}
