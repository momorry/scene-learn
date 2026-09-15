package com.moli.scene.learn.service.cache.four;

import com.moli.scene.learn.common.dao.entity.TProduct;
import com.moli.scene.learn.common.dao.mapper.TProductMapper;
import com.moli.scene.learn.common.redis.RedisUtil;
import com.moli.scene.learn.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 2 缓存击穿：互斥锁方案（Redis SETNX）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final String LOCK_KEY_PREFIX = "lock:product:";
    private static final int LOCK_EXPIRE_TIME = 10;
    private static final int CACHE_EXPIRE_TIME = 30;
    private final RedisUtil redisUtil;
    private final TProductMapper tProductMapper;

    public TProduct queryProductById(Long productId) {
        Object o = redisUtil.get(productId + "");
        if (o != null && !Objects.equals(o.toString(), "")) {
            return JsonUtil.string2Obj(JsonUtil.obj2String(o), TProduct.class);
        }
        if(o != null && Objects.equals(o.toString(), "")) {
            return null;
        }

        //加锁
        String lockKey = LOCK_KEY_PREFIX + productId;
        boolean acquired = redisUtil.setNx(lockKey, 1, LOCK_EXPIRE_TIME);
        if (acquired) {
            try {
                //重试
                o = redisUtil.get(productId + "");
                if (o != null && !Objects.equals(o.toString(), "")) {
                    return JsonUtil.string2Obj(JsonUtil.obj2String(o), TProduct.class);
                }
                if(o != null && Objects.equals(o.toString(), "")) {
                    return null;
                }
                //都数据库
                TProduct product = tProductMapper.selectById(productId);
                if (product == null) {
                    //缓存空值，避免缓存穿透
                    redisUtil.set(CACHE_KEY_PREFIX + productId, "", CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                    return null;
                }
                redisUtil.set(CACHE_KEY_PREFIX + productId, product, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                return product;
            } finally {
                redisUtil.delete(lockKey);
            }
        } else {
            // 7. 没拿到锁，等待50ms后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return queryProductById(productId);
        }
    }
}
