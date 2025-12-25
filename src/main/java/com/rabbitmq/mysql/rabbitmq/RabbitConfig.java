package com.rabbitmq.mysql.rabbitmq;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.uri}")
    private String rabbitUri;

    @Bean
    public ConnectionFactory connectionFactory() throws URISyntaxException {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setUri(new URI(rabbitUri));

        return factory;
    }
}