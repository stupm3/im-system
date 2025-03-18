package com.stupm.service.config;

import com.stupm.common.constant.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange messageExchange() {
        return new DirectExchange(Constants.RabbitConstants.Im2MessageService);
    }

    @Bean
    public DirectExchange groupExchange() {
        return new DirectExchange(Constants.RabbitConstants.Im2GroupService);
    }

    @Bean
    public DirectExchange friendExchange() {
        return new DirectExchange(Constants.RabbitConstants.Im2FriendshipService);
    }

    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(Constants.RabbitConstants.Im2UserService);
    }

    @Bean
    public Queue messageQueue() {
        return new Queue(Constants.RabbitConstants.Im2MessageService);
    }

    @Bean
    public Queue groupQueue() {
        return new Queue(Constants.RabbitConstants.Im2GroupService);
    }

    @Bean
    public Queue friendQueue() {
        return new Queue(Constants.RabbitConstants.Im2FriendshipService);
    }

    @Bean
    public Queue userQueue() {
        return new Queue(Constants.RabbitConstants.Im2UserService);
    }

    @Bean
    public Binding userBinding() {
        return BindingBuilder.bind(userQueue()).to(userExchange()).with("");
    }

    @Bean
    public Binding groupBinding() {
        return BindingBuilder.bind(groupQueue()).to(groupExchange()).with("");
    }

    @Bean
    public Binding friendBinding() {
        return BindingBuilder.bind(friendQueue()).to(friendExchange()).with("");
    }

    @Bean
    public Binding messageBinding() {
        return BindingBuilder.bind(messageQueue()).to(messageExchange()).with("");
    }

}
