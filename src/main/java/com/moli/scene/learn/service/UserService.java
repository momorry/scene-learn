package com.moli.scene.learn.service;

import com.moli.scene.learn.common.dao.entity.TUsr;
import com.moli.scene.learn.common.dao.manager.TUsrManager;
import com.moli.scene.learn.common.redis.RedisUtil;
import com.moli.scene.learn.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    public static final String UINFO_CACHE_KEY = "userinfo:id:%s";
    private final TUsrManager tUsrManager;
    private final RedisUtil redisUtil;
    /**
     * 测试使用，实际上需要自己管理一个
     */
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Transactional(rollbackFor = Exception.class)
    public void updateUserDelayDoubleDelete(Long userId, String userName) {
        String uKey = String.format(UINFO_CACHE_KEY, userId);
        redisUtil.delete(uKey);
        log.info("第一次删除缓存：{}", uKey);
        TUsr t = new TUsr();
        t.setId(userId);
        t.setUserName(userName);
        tUsrManager.updateById(t);
        executor.schedule(() -> {
            redisUtil.delete(uKey);
            log.info("延迟1秒删除:{}", uKey);
        }, 1, TimeUnit.SECONDS);
    }

    public TUsr queryUserById(Long userId) {
        Object o = redisUtil.get(String.format(UINFO_CACHE_KEY, userId));
        if (o != null) {
            TUsr tUsr = JsonUtil.string2Obj(JsonUtil.obj2String(o), TUsr.class);
            log.info("命中缓存：{} {}", String.format(UINFO_CACHE_KEY, userId), tUsr.getUserName());
            return tUsr;
        }
        TUsr byId = tUsrManager.getById(userId);
        redisUtil.set(String.format(UINFO_CACHE_KEY, userId), byId, 60);
        log.info("数据库：{}", byId);
        return byId;
    }


}
