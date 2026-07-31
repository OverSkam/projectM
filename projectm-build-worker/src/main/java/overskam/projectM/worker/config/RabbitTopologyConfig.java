package overskam.projectM.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import overskam.projectM.common.mq.RabbitMqNames;

@Configuration
public class RabbitTopologyConfig {
    
    @Bean
    public DirectExchange compileExchange() {
        return new DirectExchange(RabbitMqNames.COMPILE_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue compileTaskQueue() {
        return QueueBuilder.durable(RabbitMqNames.COMPILE_TASK_QUEUE)
                .deadLetterExchange(RabbitMqNames.COMPILE_DLX)
                .deadLetterRoutingKey(RabbitMqNames.COMPILE_DLQ_ROUTING_KEY)
                .build();
    }
    
    @Bean
    public Binding compileTaskBinding() {
        return BindingBuilder.bind(compileTaskQueue())
                .to(compileExchange())
                .with(RabbitMqNames.COMPILE_REQUEST_ROUTING_KEY);
    }
    
    @Bean
    public DirectExchange compileDlx() {
        return new DirectExchange(RabbitMqNames.COMPILE_DLX, true, false);
    }
    
    @Bean
    public Queue compileDlq() {
        return QueueBuilder.durable(RabbitMqNames.COMPILE_DLQ).build();
    }
    
    @Bean
    public Binding compileDlqBinding() {
        return BindingBuilder.bind(compileDlq())
                .to(compileDlx())
                .with(RabbitMqNames.COMPILE_DLQ_ROUTING_KEY);
    }
    
    @Bean
    public DirectExchange cleanupExchange() {
        return new DirectExchange(RabbitMqNames.CLEANUP_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue cleanupQueue() {
        return QueueBuilder.durable(RabbitMqNames.CLEANUP_QUEUE)
                .deadLetterExchange(RabbitMqNames.CLEANUP_DLX)
                .deadLetterRoutingKey(RabbitMqNames.CLEANUP_DLQ_ROUTING_KEY)
                .build();
    }
    
    @Bean
    public Binding cleanupBinding() {
        return BindingBuilder.bind(cleanupQueue())
                .to(cleanupExchange())
                .with(RabbitMqNames.PROJECT_DELETED_ROUTING_KEY);
    }
    
    @Bean
    public DirectExchange cleanupDlx() {
        return new DirectExchange(RabbitMqNames.CLEANUP_DLX, true, false);
    }
    
    @Bean
    public Queue cleanupDlq() {
        return QueueBuilder.durable(RabbitMqNames.CLEANUP_DLQ).build();
    }
    
    @Bean
    public Binding cleanupDlqBinding() {
        return BindingBuilder.bind(cleanupDlq())
                .to(cleanupDlx())
                .with(RabbitMqNames.CLEANUP_DLQ_ROUTING_KEY);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}