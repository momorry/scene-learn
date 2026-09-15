package com.moli.scene.learn.service;

import com.moli.scene.learn.common.domain.CacheDeleteMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 生产者
@Service
@Slf4j
public class CacheDeleteProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendDeleteMessage(String cacheKey) {
        try {
            // 消息体包含唯一ID，用于幂等性
            CacheDeleteMessage message = new CacheDeleteMessage(
                UUID.randomUUID().toString(),
                cacheKey
            );
            rabbitTemplate.convertAndSend("cache-exchange", "cache.delete", message);
            log.info("发送删除缓存消息成功: {}", message);
        } catch (Exception e) {
            log.error("发送删除缓存消息失败: {}", cacheKey, e);
            throw new RuntimeException("发送缓存删除消息失败", e);
        }
    }
}