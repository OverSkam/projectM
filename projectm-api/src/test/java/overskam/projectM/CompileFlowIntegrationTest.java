package overskam.projectM;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import overskam.projectM.common.dto.CompileTaskMessage;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.common.mq.RabbitMqNames;
import overskam.projectM.dto.BuildQueuedResponse;
import overskam.projectM.model.Project;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.service.BuildQueuePublisher;
import overskam.projectM.service.PluginBuildService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtb25seS0zMi1ieXRlcw")
class CompileFlowIntegrationTest extends AbstractIntegrationTest {
    
    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");
    
    @Autowired
    private PluginBuildService pluginBuildService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @MockitoSpyBean
    private BuildQueuePublisher buildQueuePublisher;
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @TestConfiguration
    static class TestQueue {
        
        @Bean
        Queue compileTestQueue() {
            return QueueBuilder.durable(RabbitMqNames.COMPILE_TASK_QUEUE).build();
        }
        
        @Bean
        Binding compileTestBinding(Queue compileTestQueue, DirectExchange compileExchange) {
            return BindingBuilder.bind(compileTestQueue)
                    .to(compileExchange)
                    .with(RabbitMqNames.COMPILE_REQUEST_ROUTING_KEY);
        }
    }
    
    @Test
    @DisplayName("Puts the compile task on the queue after the build is saved")
    void publishesCompileMessageToTheQueue() {
        UUID ownerId = UUID.randomUUID();
        Project project = new Project();
        project.setOwnerId(ownerId);
        project.setName(ownerId.toString());
        String projectId = projectRepository.save(project).getId();
        
        BuildQueuedResponse response = pluginBuildService.buildProject(ownerId, projectId);
        
        CompileTaskMessage received = rabbitTemplate.receiveAndConvert(
                RabbitMqNames.COMPILE_TASK_QUEUE, 5000,
                new ParameterizedTypeReference<>() {});
        
        assertNotNull(received);
        assertEquals(new CompileTaskMessage(response.buildId(), projectId, ownerId), received);
    }
    
    @Test
    @DisplayName("Saves new build even if broker is down")
    void savesNewBuildEvenIfBrokerDown() {
        UUID ownerId = UUID.randomUUID();
        Project project = new Project();
        project.setOwnerId(ownerId);
        project.setName(ownerId.toString());
        String projectId = projectRepository.save(project).getId();
        
        doThrow(new AmqpException("broker down")).when(buildQueuePublisher).publish(any());
        
        assertThrowsExactly(AmqpException.class, () -> pluginBuildService.buildProject(ownerId, projectId));
        assertTrue(pluginBuildRepository.existsByProjectIdAndStatusIn(projectId, List.of(BuildStatus.QUEUED)));
    }
}
