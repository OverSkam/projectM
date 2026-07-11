package overskam.projectM.worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import overskam.projectM.common.mq.RabbitMqNames;

@Configuration
public class RabbitMqConfig {
    
    @Bean
    public DirectExchange compileExchange() {
        return new DirectExchange(RabbitMqNames.COMPILE_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue compileTaskQueue() {
        return new Queue(RabbitMqNames.COMPILE_TASK_QUEUE, true);
    }
    
    @Bean
    public Binding compileTaskBinding() {
        return BindingBuilder.bind(compileTaskQueue())
                .to(compileExchange())
                .with(RabbitMqNames.COMPILE_REQUEST_ROUTING_KEY);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(MessageConverter jsonMessageConverter) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(jsonMessageConverter);
    }
}