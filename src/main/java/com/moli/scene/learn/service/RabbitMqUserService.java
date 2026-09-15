package com.moli.scene.learn.service;

import com.moli.scene.learn.common.dao.entity.TUsr;
import com.moli.scene.learn.common.dao.manager.TUsrManager;
import com.moli.scene.learn.common.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMqUserService {

    public static final String UINFO_CACHE_KEY = "mq:userinfo:id:%s";
    private final TUsrManager tUsrManager;
    private final RedisUtil redisUtil;

    private final CacheDeleteProducer cacheDeleteProducer;

    @Transactional(rollbackFor = Exception.class)
    public void updateUserMq(Long userId, String userName) {
        String uKey = String.format(UINFO_CACHE_KEY, userId);
        cacheDeleteProducer.sendDeleteMessage(uKey);
        TUsr t = new TUsr();
        t.setId(userId);
        t.setUserName(userName);
        tUsrManager.updateById(t);
        cacheDeleteProducer.sendDeleteMessage(uKey);
    }

    public TUsr queryUserById(Long userId) {
        TUsr tUser = (TUsr) redisUtil.get(String.format(UINFO_CACHE_KEY, userId));
        if(tUser != null) {
            log.info("命中缓存：{} {}", String.format(UINFO_CACHE_KEY, userId), tUser.getUserName());
            return tUser;
        }
        TUsr byId = tUsrManager.getById(userId);
        redisUtil.set(String.format(UINFO_CACHE_KEY, userId), byId, 60);
        log.info("数据库：{}", byId);
        return byId;
    }
}
