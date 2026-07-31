package overskam.projectM.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import overskam.projectM.common.dto.ProjectDeletedMessage;
import overskam.projectM.common.mq.RabbitMqNames;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupTaskPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public void publishProjectDeleted(ProjectDeletedMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqNames.CLEANUP_EXCHANGE,
                RabbitMqNames.PROJECT_DELETED_ROUTING_KEY,
                message
        );
    }
}
