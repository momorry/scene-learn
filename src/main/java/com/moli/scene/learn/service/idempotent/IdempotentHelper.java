package com.moli.scene.learn.service.idempotent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IdempotentHelper {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_PREFIX = "idempotent:order:";

    /**
     * 尝试获取幂等锁
     * @param requestId 业务请求唯一标识（如雪花ID、UUID）
     * @param expireSeconds 过期时间（秒），建议大于业务最大耗时+网络抖动余量
     * @return true=首次请求，放行；false=重复请求，拦截
     */
    public boolean tryAcquire(String requestId, long expireSeconds) {
        String key = IDEMPOTENT_PREFIX + requestId;
        // 核心：SET key value NX EX seconds 单命令原子操作
        // setIfAbsent 对应 Redis 的 SETNX，同时设置过期时间防止死锁
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放幂等锁（可选，用于业务执行失败时允许重试）
     * @param requestId 业务请求唯一标识
     */
    public void release(String requestId) {
        String key = IDEMPOTENT_PREFIX + requestId;
        redisTemplate.delete(key);
    }
}