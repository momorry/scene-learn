package com.moli.scene.learn.common.redis;

import com.moli.scene.learn.common.holder.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;

/**
 * @author moli
 **/
@Slf4j
public class RedissonUtils {

    private static volatile RedissonClient client;

    private static RedissonClient getClient() {
        if (client == null) {
            synchronized(RedissonClient.class){
                if( client ==null) {
                    client = SpringContextHolder.getBean(RedissonClient.class);
                }
            }
        }
        return client;
    }
    /**
     * 获取分布式公平读 锁
     *
     * @param lockName 锁的名称
     * @return
     */
    public static RLock getFairLock(String lockPrefix,String lockName) {
        if(StringUtils.isBlank(lockPrefix) || StringUtils.isBlank(lockName)) {
            throw new IllegalArgumentException("lockPrefix,lockName 必填");
        }
        return getClient().getFairLock(lockPrefix+":"+lockName);
    }

    public static <K, V> RMap<K, V> getMap(String name) {
        return getClient().getMap(name);
    }

    public static <T> RList<T> getList(String name) {
        return getClient().getList(name);
    }

    public static RAtomicLong getRAtomicLong(String name) {
        return getClient().getAtomicLong(name);
    }

}
