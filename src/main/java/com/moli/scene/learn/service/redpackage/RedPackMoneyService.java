package com.moli.scene.learn.service.redpackage;

import com.moli.scene.learn.common.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 抢红包服务
 * <p>
 * 完整流程：发红包(扣余额) → 抢红包(二倍均值算法) → 入账(加余额)
 * <p>
 * Redis Key 设计（单位均为分）：
 * <ul>
 *   <li>redpack:balance:{userId}       — 用户余额</li>
 *   <li>redpack:{redPackId}:stock      — 红包剩余库存</li>
 *   <li>redpack:{redPackId}:remain     — 红包剩余金额(分)</li>
 *   <li>redpack:{redPackId}:grabbed    — 已抢用户集合(Set)</li>
 *   <li>redpack:{redPackId}:detail     — 每人抢到的金额(Hash: userId -> amount)</li>
 *   <li>redpack:{redPackId}:info       — 红包基础信息(Hash: senderId/count/totalAmount)</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedPackMoneyService {

    private static final String BALANCE_KEY_PREFIX = "redpack:balance:";
    private static final String REDPACK_KEY_PREFIX = "redpack:";

    private final RedisUtil redisUtil;
    private final StringRedisTemplate stringRedisTemplate;

    /** 发红包 Lua 脚本内容（缓存加载一次） */
    private volatile String deliveryScript;
    /** 抢红包 Lua 脚本内容（缓存加载一次） */
    private volatile String acquireScript;

    // ============================= 发红包 =============================

    /**
     * 发放红包：扣减发送者余额，初始化红包库存和金额
     *
     * @param senderId 发送者用户ID
     * @param count    红包个数
     * @param amount   红包总金额（单位：分）
     * @return 红包ID，失败返回 null
     */
    public String deliveryRedPack(Integer senderId, int count, int amount) {
        String redPackId = generateRedPackId();
        String balanceKey = BALANCE_KEY_PREFIX + senderId;
        String stockKey = REDPACK_KEY_PREFIX + redPackId + ":stock";
        String remainKey = REDPACK_KEY_PREFIX + redPackId + ":remain";

        // 初始化用户余额（如果不存在则设为 0）
        stringRedisTemplate.opsForValue().setIfAbsent(balanceKey, "0");

        try {
            String script = loadDeliveryScript();
            Long result = redisUtil.executeScript(
                    script, Long.class,
                    List.of(balanceKey, stockKey, remainKey),
                    String.valueOf(amount), String.valueOf(count)
            );

            if (result == null || result < 0) {
                log.warn("发红包失败, senderId={}, count={}, amount={}, code={}", senderId, count, amount, result);
                return null;
            }

            // 保存红包基础信息（用于查询）
            String infoKey = REDPACK_KEY_PREFIX + redPackId + ":info";
            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("senderId", String.valueOf(senderId));
            infoMap.put("count", String.valueOf(count));
            infoMap.put("totalAmount", String.valueOf(amount));
            stringRedisTemplate.opsForHash().putAll(infoKey, infoMap);

            log.info("发红包成功, redPackId={}, senderId={}, count={}, amount(分)={}", redPackId, senderId, count, amount);
            return redPackId;

        } catch (Exception e) {
            log.error("发红包异常, senderId={}, count={}, amount={}", senderId, count, amount, e);
            // 回滚：清理已初始化的 key
            redisUtil.delete(List.of(stockKey, remainKey));
            return null;
        }
    }

    // ============================= 抢红包 =============================

    /**
     * 抢红包：二倍均值算法拆分金额，扣减库存，增加用户余额
     *
     * @param redPackId 红包ID
     * @param userId    用户ID
     * @return 抢到的金额（单位：分），-1=重复抢，-2=已抢光，-3=红包不存在，null=异常
     */
    public Long acquireRedPack(String redPackId, Integer userId) {
        String stockKey = REDPACK_KEY_PREFIX + redPackId + ":stock";
        String userSetKey = REDPACK_KEY_PREFIX + redPackId + ":grabbed";
        String remainKey = REDPACK_KEY_PREFIX + redPackId + ":remain";
        String balanceKey = BALANCE_KEY_PREFIX + userId;
        String detailKey = REDPACK_KEY_PREFIX + redPackId + ":detail";

        // 检查红包是否存在
        if (!redisUtil.hasKey(stockKey)) {
            log.warn("红包不存在, redPackId={}", redPackId);
            return -3L;
        }

        // 获取当前剩余人数（用于二倍均值计算）
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if (stockStr == null) {
            return -3L;
        }
        long remainCount = Long.parseLong(stockStr);
        if (remainCount <= 0) {
            return -2L;
        }

        // 初始化抢红包用户的余额 key（如果不存在）
        stringRedisTemplate.opsForValue().setIfAbsent(balanceKey, "0");

        try {
            String script = loadAcquireScript();
            Long grabbedAmount = redisUtil.executeScript(
                    script, Long.class,
                    List.of(stockKey, userSetKey, remainKey),
                    String.valueOf(userId), "1", String.valueOf(remainCount)
            );

            if (grabbedAmount == null) {
                return null;
            }
            if (grabbedAmount == -1) {
                log.info("用户重复抢红包, redPackId={}, userId={}", redPackId, userId);
                return -1L;
            }
            if (grabbedAmount == 0) {
                log.info("红包已抢光, redPackId={}, userId={}", redPackId, userId);
                return -2L;
            }

            // 抢到的金额入账到用户余额
            redisUtil.increment(balanceKey, grabbedAmount);

            // 记录每人抢到的金额明细
            stringRedisTemplate.opsForHash().put(detailKey, String.valueOf(userId), String.valueOf(grabbedAmount));

            log.info("抢红包成功, redPackId={}, userId={}, amount(分)={}", redPackId, userId, grabbedAmount);
            return grabbedAmount;

        } catch (Exception e) {
            log.error("抢红包异常, redPackId={}, userId={}", redPackId, userId, e);
            return null;
        }
    }

    // ============================= 查询 =============================

    /**
     * 查询用户余额（单位：分）
     */
    public Long queryBalance(Integer userId) {
        String balanceKey = BALANCE_KEY_PREFIX + userId;
        String balance = stringRedisTemplate.opsForValue().get(balanceKey);
        return balance != null ? Long.parseLong(balance) : 0L;
    }

    /**
     * 设置用户余额（用于测试/初始化）
     */
    public void setBalance(Integer userId, long amount) {
        String balanceKey = BALANCE_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(balanceKey, String.valueOf(amount));
    }

    /**
     * 查询红包详情（发放信息 + 每人抢到的金额明细）
     */
    public Map<String, Object> queryRedPackDetail(String redPackId) {
        Map<String, Object> result = new HashMap<>();

        // 红包基础信息
        String infoKey = REDPACK_KEY_PREFIX + redPackId + ":info";
        Map<Object, Object> info = stringRedisTemplate.opsForHash().entries(infoKey);
        if (info.isEmpty()) {
            return null;
        }
        result.put("redPackId", redPackId);
        result.put("senderId", info.get("senderId"));
        result.put("count", info.get("count"));
        result.put("totalAmount", info.get("totalAmount"));

        // 剩余库存和金额
        String stockKey = REDPACK_KEY_PREFIX + redPackId + ":stock";
        String remainKey = REDPACK_KEY_PREFIX + redPackId + ":remain";
        String stock = stringRedisTemplate.opsForValue().get(stockKey);
        String remain = stringRedisTemplate.opsForValue().get(remainKey);
        result.put("remainStock", stock != null ? stock : "0");
        result.put("remainAmount", remain != null ? remain : "0");

        // 每人抢到的金额明细
        String detailKey = REDPACK_KEY_PREFIX + redPackId + ":detail";
        Map<Object, Object> detail = stringRedisTemplate.opsForHash().entries(detailKey);
        result.put("grabDetail", detail);

        return result;
    }

    // ============================= 内部方法 =============================

    /**
     * 生成红包ID
     */
    private String generateRedPackId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 加载发红包 Lua 脚本（双重检查缓存）
     */
    private String loadDeliveryScript() throws IOException {
        if (deliveryScript == null) {
            synchronized (this) {
                if (deliveryScript == null) {
                    deliveryScript = loadScriptFromClasspath("scripts/deliveryRedPack.lua");
                }
            }
        }
        return deliveryScript;
    }

    /**
     * 加载抢红包 Lua 脚本（双重检查缓存）
     */
    private String loadAcquireScript() throws IOException {
        if (acquireScript == null) {
            synchronized (this) {
                if (acquireScript == null) {
                    acquireScript = loadScriptFromClasspath("scripts/acquireRedPack.lua");
                }
            }
        }
        return acquireScript;
    }

    /**
     * 从 classpath 加载 Lua 脚本内容
     */
    private String loadScriptFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
