package overskam.projectM.worker.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import overskam.projectM.common.dto.CompileTaskMessage;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.common.mq.RabbitMqNames;
import overskam.projectM.worker.AbstractWorkerIntegrationTest;
import overskam.projectM.worker.model.PluginBuild;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;
import overskam.projectM.worker.repository.mongo.ProjectRepository;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.mongodb.auto-index-creation=false"
})
class CompileTaskListenerTest extends AbstractWorkerIntegrationTest {
    
    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");
    
    @MockitoBean
    private ProjectRepository projectRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @Test
    @DisplayName("A failed compile ends in the dead-letter queue")
    void failedCompilingGoesToDeadLetterQueue() {
        UUID ownerId = UUID.randomUUID();
        String projectId = UUID.randomUUID().toString();
        PluginBuild build = new PluginBuild();
        build.setOwnerId(ownerId);
        build.setProjectId(projectId);
        build.setStatus(BuildStatus.QUEUED);
        UUID buildId = pluginBuildRepository.save(build).getId();
        
        when(projectRepository.findByIdAndOwnerId(projectId, ownerId)).thenThrow(new RuntimeException());
        
        rabbitTemplate.convertAndSend(
                RabbitMqNames.COMPILE_EXCHANGE,
                RabbitMqNames.COMPILE_REQUEST_ROUTING_KEY,
                new CompileTaskMessage(buildId, projectId, ownerId)
        );
        
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertNotNull(rabbitTemplate.receiveAndConvert(
                        RabbitMqNames.COMPILE_DLQ, 500,
                        new ParameterizedTypeReference<CompileTaskMessage>() {})));
        
        assertEquals(BuildStatus.QUEUED, pluginBuildRepository.findByProjectId(projectId).getFirst().getStatus());
        verify(projectRepository, atLeast(2)).findByIdAndOwnerId(any(), any());
    }
}
