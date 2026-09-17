package com.moli.scene.learn.service.cache.six;

/**
 * 技术亮点：自定义哈希标签工具类
 * 将相同业务的key强制路由到同一个哈希槽，支持MGET/MSET等多键操作
 */
public class RedisHashTagUtil {
    // 哈希标签分隔符
    private static final String HASH_TAG_START = "{";
    private static final String HASH_TAG_END = "}";

    /**
     * 生成带哈希标签的key
     * @param businessId 业务ID（如用户ID、订单ID）
     * @param keySuffix key后缀
     * @return 带哈希标签的key
     */
    public static String generateKey(String businessId, String keySuffix) {
        return HASH_TAG_START + businessId + HASH_TAG_END + ":" + keySuffix;
    }

    // 使用示例
    public static void main(String[] args) {
        // 同一个用户的所有key都会路由到同一个哈希槽
        String userInfoKey = generateKey("user123", "info");
        String userOrderKey = generateKey("user123", "orders");
        String userCartKey = generateKey("user123", "cart");

        // 现在可以安全地执行MGET操作
        // redisTemplate.opsForValue().multiGet(Arrays.asList(userInfoKey, userOrderKey, userCartKey));
    }
}
