package com.moli.scene.learn.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * <p>封装常用 Redis 操作，包括 String、Hash、List、Set、Key 等</p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    // ============================= Key 操作 =============================

    /**
     * 设置 key 的过期时间
     *
     * @param key     键
     * @param timeout 超时时间（秒）
     * @return 是否设置成功
     */
    public boolean expire(String key, long timeout) {
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置 key 的过期时间
     *
     * @param key     键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取 key 的过期时间（秒），-1 表示永不过期，-2 表示 key 不存在
     *
     * @param key 键
     * @return 过期时间（秒）
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2;
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 删除 key（单个）
     *
     * @param key 键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 批量删除 key
     *
     * @param keys 键集合
     * @return 删除的个数
     */
    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }

    // ============================= String 操作 =============================

    /**
     * 设置字符串值
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置字符串值并指定过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
    }

    /**
     * 设置字符串值并指定过期时间和单位
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } else {
            set(key, value);
        }
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置 String 类型值
     *
     * @param key   键
     * @param value 字符串值
     */
    public void setStr(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 String 类型值并指定过期时间
     *
     * @param key     键
     * @param value   字符串值
     * @param timeout 过期时间（秒）
     */
    public void setStr(String key, String value, long timeout) {
        if (timeout > 0) {
            stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } else {
            setStr(key, value);
        }
    }

    /**
     * 获取 String 类型值
     *
     * @param key 键
     * @return 字符串值
     */
    public String getStr(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 递增因子（必须大于0）
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 递减因子（必须大于0）
     * @return 递减后的值
     */
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, -delta);
        return result != null ? result : 0;
    }

    /**
     * setNx（key 不存在时才设置）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean setNx(String key, Object value, long timeout) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    // ============================= Hash 操作 =============================

    /**
     * Hash 设置单个字段
     *
     * @param key     键
     * @param field   字段
     * @param value   值
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash 批量设置
     *
     * @param key 键
     * @param map 字段-值 Map
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * Hash 获取单个字段值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Hash 获取所有字段和值
     *
     * @param key 键
     * @return 字段-值 Map
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash 删除字段
     *
     * @param key    键
     * @param fields 字段（可多个）
     * @return 删除的字段数
     */
    public long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * Hash 判断字段是否存在
     *
     * @param key   键
     * @param field 字段
     * @return 是否存在
     */
    public boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * Hash 字段递增
     *
     * @param key   键
     * @param field 字段
     * @param delta 递增量
     * @return 递增后的值
     */
    public long hIncrement(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    // ============================= List 操作 =============================

    /**
     * List 右推入
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public long lRightPush(String key, Object value) {
        Long result = redisTemplate.opsForList().rightPush(key, value);
        return result != null ? result : 0;
    }

    /**
     * List 批量右推入
     *
     * @param key    键
     * @param values 值列表
     * @return 列表长度
     */
    public long lRightPushAll(String key, List<Object> values) {
        Long result = redisTemplate.opsForList().rightPushAll(key, values);
        return result != null ? result : 0;
    }

    /**
     * List 左弹出
     *
     * @param key 键
     * @return 弹出的值
     */
    public Object lLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * List 范围获取
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 值列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * List 获取长度
     *
     * @param key 键
     * @return 长度
     */
    public long lSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    // ============================= Set 操作 =============================

    /**
     * Set 添加元素
     *
     * @param key    键
     * @param values 值（可多个）
     * @return 添加的个数
     */
    public long sAdd(String key, Object... values) {
        Long result = redisTemplate.opsForSet().add(key, values);
        return result != null ? result : 0;
    }

    /**
     * Set 获取所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Set 判断是否包含
     *
     * @param key   键
     * @param value 值
     * @return 是否包含
     */
    public boolean sIsMember(String key, Object value) {
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Set 获取大小
     *
     * @param key 键
     * @return 大小
     */
    public long sSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * Set 移除元素
     *
     * @param key    键
     * @param values 值（可多个）
     * @return 移除的个数
     */
    public long sRemove(String key, Object... values) {
        Long result = redisTemplate.opsForSet().remove(key, values);
        return result != null ? result : 0;
    }

    // ============================= Script 操作 =============================

    /**
     * 执行 Lua 脚本
     *
     * @param script 脚本内容
     * @param keys   KEYS 列表
     * @param args   ARGV 列表
     * @return 脚本返回值
     */
    @SuppressWarnings("unchecked")
    public <T> T executeScript(String script, Class<T> resultType, List<String> keys, Object... args) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>(script, resultType);
        return (T) stringRedisTemplate.execute(redisScript, keys, args);
    }
}
