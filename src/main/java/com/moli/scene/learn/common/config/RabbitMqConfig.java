package com.moli.scene.learn.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CACHE_EXCHANGE = "cache-exchange";
    public static final String CACHE_DELETE_QUEUE = "cache-delete-queue";
    public static final String ROUTING_KEY = "cache.delete";

    @Bean
    public DirectExchange cacheExchange() {
        return new DirectExchange(CACHE_EXCHANGE);
    }

    @Bean
    public Queue cacheDeleteQueue() {
        return QueueBuilder.durable(CACHE_DELETE_QUEUE).build();
    }

    /**
     * Producer 往 cache-exchange 发消息，Consumer 监听 cache-delete-queue，
     * 但没有任何配置把它们绑定在一起。消息发出去后无法路由到队列，Consumer 永远收不到消息，
     * 缓存永远不会被删除。
     *
     * @param cacheDeleteQueue
     * @param cacheExchange
     * @return
     */
    @Bean
    public Binding cacheDeleteBinding(Queue cacheDeleteQueue, DirectExchange cacheExchange) {
        return BindingBuilder.bind(cacheDeleteQueue).to(cacheExchange).with(ROUTING_KEY);
    }

    /**
     * JSON 消息转换器，替代默认的 Java 序列化，避免反序列化失败
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
