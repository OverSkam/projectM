package overskam.projectM.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.dto.BuildRequestMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildQueuePublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public void publish(BuildRequestMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.BUILD_REQUEST_QUEUE,
                message
        );
    }
}
