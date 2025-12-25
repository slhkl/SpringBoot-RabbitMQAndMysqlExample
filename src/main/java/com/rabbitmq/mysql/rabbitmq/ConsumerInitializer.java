package com.rabbitmq.mysql.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.mysql.dto.ConsumeDto;
import com.rabbitmq.mysql.entities.Company;
import com.rabbitmq.mysql.mysql.repositories.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
public class ConsumerInitializer {
    private final Logger logger = LoggerFactory.getLogger(ConsumerInitializer.class);

    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    private final Set<String> activeSerials = new ConcurrentSkipListSet<>();

    public ConsumerInitializer(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin, CompanyRepository companyRepository, ObjectMapper objectMapper) {
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 10000, initialDelay = 10000)
    public void scheduledCheck() {
        checkAndCreateListeners();
    }

    private void checkAndCreateListeners() {
        var companies = companyRepository.findAll();
        for (Company company : companies) {
            if (!activeSerials.contains(company.getSerial())) {
                logger.info("Yeni cihaz tespit edildi: {}. Listener açılıyor...", company.getSerial());
                createListenerForSerial(company.getSerial());
            } else {
                logger.info("Zaten çalışıyor: {}", company.getSerial());
            }
        }
    }

    public void createListenerForSerial(String serial) {
        activeSerials.add(serial);

        String queueName = "Serial-" + serial;

        Queue queue = new Queue(queueName, true, false, false);
        amqpAdmin.declareQueue(queue);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setPrefetchCount(1);
        container.setConcurrentConsumers(1);
        container.setDefaultRequeueRejected(false);
        container.setRetryDeclarationInterval(3000L);
        container.setQueueNames(queueName);
        container.setMissingQueuesFatal(false);

        container.setMessageListener(message -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                processMessage(serial, objectMapper.readValue(body, ConsumeDto.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        container.start();
    }

    private void processMessage(String serial, ConsumeDto content) {
        logger.info("Receive message from queue {}", serial);
        logger.info(content.getData());
    }
}

