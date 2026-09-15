package com.moli.scene.learn.service.cache.four;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moli.scene.learn.common.dao.entity.TProduct;
import com.moli.scene.learn.common.dao.mapper.TProductMapper;
import com.moli.scene.learn.common.redis.RedisUtil;
import com.moli.scene.learn.common.redis.RedissonUtils;
import com.moli.scene.learn.common.util.JsonUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotProductService {

    private final RedisUtil redisUtil;
    private final TProductMapper tProductMapper;

    private static final String CACHE_KEY_PREFIX = "hot:product:";
    private static final int LOGIC_EXPIRE_TIME = 30 * 60; // 逻辑过期时间30分钟

    // 线程池，用于异步更新缓存
    private static final ExecutorService CACHE_REFRESH_EXECUTOR =
            Executors.newFixedThreadPool(10);

    // 缓存数据对象，包含逻辑过期时间
    @Data
    private static class CacheData<T> {
        private T data;
        private Long expireTime; // 逻辑过期时间戳

        public CacheData(T data, Long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }

        // 判断是否过期
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public TProduct getHotProductById(String productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;

        // 1. 查询缓存
        String cacheJson = redisUtil.getStr(cacheKey);
        if (cacheJson == null) {
            return null; // 热点key提前预热，理论上不会走到这里
        }

        CacheData<TProduct> cacheData = JsonUtil.string2Obj(cacheJson, new TypeReference<>() {
        });
        TProduct product = cacheData.getData();

        // 2. 判断是否逻辑过期
        if (!cacheData.isExpired()) {
            return product; // 未过期，直接返回
        }

        // 3. 已过期，异步更新缓存
        String lockKey = "lock:hot:product:" + productId;
        boolean lockAcquired = redisUtil
                .setNx(lockKey, "1", 10);

        if (lockAcquired) {
            // 提交异步任务更新缓存
            CACHE_REFRESH_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    TProduct newProduct = tProductMapper.selectById(productId);
                    // 更新缓存，设置新的逻辑过期时间
                    CacheData<TProduct> newCacheData = new CacheData<>(
                            newProduct,
                            System.currentTimeMillis() + LOGIC_EXPIRE_TIME * 1000L
                    );
                    redisUtil.set(cacheKey, newCacheData);
                } finally {
                    redisUtil.delete(lockKey);
                }
            });
        }

        // 4. 无论是否拿到锁，都返回旧数据
        return product;
    }


    /**
     * 💥 缓存击穿 — 核心代码
     * 技术亮点：使用 Redisson 分布式锁 + 看门狗自动续期，避免死锁，保障高并发。
     *
     * @param id
     * @return
     */
    public TProduct getHotProduct(Long id) {
        String key = "hot_product:" + id;
        Object cacheData = redisUtil.get(key);
        if (cacheData != null) {
            return JsonUtil.string2Obj(JsonUtil.obj2String(cacheData), TProduct.class);
        }

        // 分布式锁key
        RLock lock = RedissonUtils.getFairLock("lock:product", id + "");
        try {
            // 尝试加锁，最多等待10秒，锁超时时间（看门狗会续期，这里设为30秒兜底）
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // 双重检查
                cacheData = redisUtil.get(key);
                if (cacheData != null) {
                    return JsonUtil.string2Obj(JsonUtil.obj2String(cacheData), TProduct.class);
                }
                // 查询数据库
                TProduct product = tProductMapper.selectById(id);
                if (product != null) {
                    // 设置缓存，加随机过期时间（300~420秒），防止雪崩
                    redisUtil.set(key, product,
                            300 + ThreadLocalRandom.current().nextInt(120), TimeUnit.SECONDS);
                } else {
                    // 空值缓存，防止穿透
                    redisUtil.set(key, "NULL_PLACEHOLDER", 30, TimeUnit.SECONDS);
                }
                return product;
            } else {
                // 获取锁失败，休眠后重试
                Thread.sleep(50);
                return getHotProduct(id); // 递归重试
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    // 本地缓存配置（Caffeine）
    Cache<Long, Object> localCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    /**
     * ❄️ 缓存雪崩 — 核心代码
     *
     * @param id
     * @return
     */
    public Object getDataWithMultiLevel(Long id) {
        String key = "data:" + id;

        // 1. 一级：本地缓存
        Object localVal = localCache.getIfPresent(id);
        if (localVal != null) {
            return localVal;
        }

        // 2. 二级：Redis
        Object cacheVal = redisUtil.get(key);
        if (cacheVal != null) {
            localCache.put(id, cacheVal); // 回填本地
            return cacheVal;
        }

        // 3. 查库并回填（带锁防止击穿）
        Object dbVal = queryFromDbWithLock(id);
        if (dbVal != null) {
            // 设置Redis过期时间，基础1小时 + 随机0~600秒
            long expireSec = 3600 + ThreadLocalRandom.current().nextInt(600);
            redisUtil.set(key, dbVal, expireSec, TimeUnit.SECONDS);
            localCache.put(id, dbVal);
        }
        return dbVal;
    }

    private Object queryFromDbWithLock(Long id) {
        //只是个代码示意
        return null;
    }


}
