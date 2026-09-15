package com.moli.scene.learn.service;

import com.moli.scene.learn.common.domain.CacheDeleteMessage;
import com.moli.scene.learn.common.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// 消费者
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheDeleteConsumer {
    private final RedisUtil redisUtil;

    @RabbitListener(queues = "cache-delete-queue")
    public void handleDeleteMessage(CacheDeleteMessage message) {
        String cacheKey = message.getCacheKey();
        String messageId = message.getMessageId();
        String idempotentKey = "cache:delete:idempotent:" + messageId;

        try {
            // 1. 原子性幂等检查：setIfAbsent 保证只有一个消费者能抢到处理权
            boolean acquired = redisUtil.setNx(idempotentKey, "1", 24 * 3600);
            if (!acquired) {
                log.info("消息已处理，跳过: {}", messageId);
                return;
            }

            // 2. 删除缓存
            redisUtil.delete(cacheKey);
            log.info("消费消息删除缓存成功: {}", cacheKey);
        } catch (Exception e) {
            // 处理失败，清除幂等标记以允许重试
            redisUtil.delete(idempotentKey);
            log.error("消费消息删除缓存失败: {}", message, e);
            throw new RuntimeException("处理缓存删除消息失败", e);
        }
    }
}