package overskam.projectM.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import overskam.projectM.common.mq.RabbitMqNames;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.common.dto.CompileTaskMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildQueuePublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public void publish(CompileTaskMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqNames.COMPILE_EXCHANGE,
                RabbitMqNames.COMPILE_REQUEST_ROUTING_KEY,
                message
        );
    }
}
