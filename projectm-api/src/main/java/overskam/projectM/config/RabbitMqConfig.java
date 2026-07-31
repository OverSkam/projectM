package overskam.projectM.config;

import org.springframework.amqp.core.DirectExchange;
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
    public DirectExchange cleanupExchange() {
        return new DirectExchange(RabbitMqNames.CLEANUP_EXCHANGE, true, false);
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