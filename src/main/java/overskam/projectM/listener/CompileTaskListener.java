package overskam.projectM.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.dto.CompileTaskMessage;

@Slf4j
@Component
public class CompileTaskListener {
    
    @RabbitListener(queues = RabbitMqConfig.COMPILE_TASK_QUEUE)
    public void handleCompileTask(CompileTaskMessage message) {
        log.info("Received compile task: {}", message);
    }
}
