package overskam.projectM.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String COMPILE_EXCHANGE = "projectm.compile.exchange";
    public static final String SAVE_EXCHANGE = "projectm.save.exchange";
    public static final String RPC_EXCHANGE = "projectm.rpc.exchange";
    
    public static final String COMPILE_TASK_QUEUE = "projectm.compile.tasks";
    public static final String SAVE_TASK_QUEUE = "projectm.save.tasks";
    public static final String RPC_REPLY_QUEUE = "projectm.rpc.replies";
    
    public static final String COMPILE_REQUEST_ROUTING_KEY = "compile.request";
    public static final String SAVE_REQUEST_ROUTING_KEY = "save.request";
    public static final String RPC_REPLY_ROUTING_KEY = "rpc.reply";
    
    @Bean
    public DirectExchange compileExchange() {
        return new DirectExchange(COMPILE_EXCHANGE, true, false);
    }
    
    @Bean
    public DirectExchange saveExchange() {
        return new DirectExchange(SAVE_EXCHANGE, true, false);
    }
    
    @Bean
    public DirectExchange rpcExchange() {
        return new DirectExchange(RPC_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue compileTaskQueue() {
        return new Queue(COMPILE_TASK_QUEUE, true);
    }
    
    @Bean
    public Queue saveTaskQueue() {
        return new Queue(SAVE_TASK_QUEUE, true);
    }
    
    @Bean
    public Queue rpcReplyQueue() {
        return new Queue(RPC_REPLY_QUEUE, true);
    }
    
    @Bean
    public Binding compileTaskBinding() {
        return BindingBuilder.bind(compileTaskQueue())
                .to(compileExchange())
                .with(COMPILE_REQUEST_ROUTING_KEY);
    }
    
    @Bean
    public Binding saveTaskBinding() {
        return BindingBuilder.bind(saveTaskQueue())
                .to(saveExchange())
                .with(SAVE_REQUEST_ROUTING_KEY);
    }
    
    @Bean
    public Binding rpcReplyBinding() {
        return BindingBuilder.bind(rpcReplyQueue())
                .to(rpcExchange())
                .with(RPC_REPLY_ROUTING_KEY);
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