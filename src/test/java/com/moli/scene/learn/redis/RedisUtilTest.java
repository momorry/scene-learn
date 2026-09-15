package com.moli.scene.learn.redis;

import com.moli.scene.learn.common.redis.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

/**
 * RedisUtil 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisUtil redisUtil;

    @BeforeEach
    void setUp() {
        redisUtil = new RedisUtil(stringRedisTemplate, redisTemplate);
    }

    @Test
    @DisplayName("set - 设置 string key 成功")
    void testSet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisUtil.set("testKey", "testValue");

        verify(valueOperations, times(1)).set("testKey", "testValue");
    }

    @Test
    @DisplayName("set - 设置 string key 并指定过期时间（秒）")
    void testSetWithTimeout() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisUtil.set("testKey", "testValue", 60);

        verify(valueOperations, times(1)).set(eq("testKey"), eq("testValue"), eq(60L), any());
    }

    @Test
    @DisplayName("set - timeout <= 0 时不设置过期时间")
    void testSetWithZeroTimeout() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisUtil.set("testKey", "testValue", 0);

        verify(valueOperations, times(1)).set("testKey", "testValue");
        verify(valueOperations, never()).set(eq("testKey"), eq("testValue"), anyLong(), any());
    }

    @Test
    @DisplayName("get - 获取 string key 的值")
    void testGet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("testKey")).thenReturn("testValue");

        Object result = redisUtil.get("testKey");

        assert result != null;
        assert result.equals("testValue");
        verify(valueOperations, times(1)).get("testKey");
    }
}
