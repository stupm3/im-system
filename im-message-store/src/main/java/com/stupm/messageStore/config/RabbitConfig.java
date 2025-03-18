package com.stupm.messageStore.config;

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
    public DirectExchange p2pExchange() {
        return new DirectExchange(Constants.RabbitConstants.StoreP2PMessage);
    }
    @Bean
    public Queue p2pQueue() {
        return new Queue(Constants.RabbitConstants.StoreP2PMessage);
    }

    @Bean
    public Binding p2pBinding() {
        return BindingBuilder.bind(p2pQueue()).to(p2pExchange()).with("");    }
    @Bean
    public DirectExchange groupExchange() {
        return new DirectExchange(Constants.RabbitConstants.StoreGroupMessage);
    }
    @Bean
    public Queue groupQueue() {
        return new Queue(Constants.RabbitConstants.StoreGroupMessage);
    }

    @Bean
    public Binding groupBinding() {
        return BindingBuilder.bind(p2pQueue()).to(p2pExchange()).with("");    }
}
