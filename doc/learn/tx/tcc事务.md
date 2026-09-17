以下为您整理的关于 **Seata TCC 多表多行新增场景** 的完整实战指南文档。

***

# Seata TCC 多表多行新增场景实战指南

## 一、 背景与痛点

在大多数 Seata TCC 的 Demo 中，通常以“库存扣减”为例。库存属于“存量”概念，天然契合 TCC “资源预留与释放”的直觉（Try 冻结，Confirm 扣减，Cancel 释放）。

但在实际复杂业务中，我们经常遇到 **“多表、多行数据新增”** 的场景（例如：电商下单时，同时新增订单主表 1 行、订单明细表 N 行、物流单表 1 行等）。对于这种“增量”场景，没有现成的存量资源可以冻结，我们需要转换思维。

## 二、 核心设计思想：状态机驱动

对于多表多行新增场景，TCC 的核心原则是：**Try 阶段不产生最终业务价值的数据，而是产生“中间状态（草稿）”的数据；Confirm/Cancel 阶段通过状态机流转来确认或撤销这些草稿。**

1. **Try 阶段**：进行业务校验，将多表多行数据插入数据库，但**状态字段标记为 `TRYING`**。
2. **Confirm 阶段**：将数据库中对应的、状态为 `TRYING` 的多表多行数据，**批量更新为 `SUCCESS`**。
3. **Cancel 阶段**：将数据库中对应的、状态为 `TRYING` 的多表多行数据，**批量更新为 `CANCELED`**（或直接物理删除）。

## 三、 业务表是否需要 `xid` 和 `branch_id`？

这是一个常见的误区。结论是：**在开启 `useTccFence=true` 的前提下，业务表不需要 `xid` 和 `branch_id` 字段。**

* **传统方案（有 xid）**：在业务表存 `xid`，Confirm/Cancel 时通过 `WHERE xid = ?` 定位数据。优点是排查问题方便，缺点是侵入业务表结构。
* **推荐方案（无 xid）**：利用业务上唯一的标识符（如 `order_no`）作为桥梁。Confirm/Cancel 时通过 `WHERE order_no = ? AND status = 'TRYING'` 定位数据。优点是业务表干净，符合领域驱动设计（DDD）原则。

## 四、 引入 `useTccFence=true` 简化开发

Seata 1.4.2+ 引入了 `useTccFence = true` 特性，极大地简化了 TCC 开发。
* **自动处理空回滚与悬挂**：Seata 会在本地数据库自动维护 `tcc_fence_log` 表，自动拦截空回滚和悬挂问题，开发者无需手动编写复杂的控制逻辑。
* **自动保证幂等性**：框架层面保证 Confirm 和 Cancel 不会被重复执行。

---

## 五、 完整代码实战

### 1. 数据库表设计

首先，需要在业务数据库中执行以下 SQL，创建业务表和 Seata 必须的 Fence 日志表。

```sql
-- 1. 订单主表 (无 xid/branch_id 字段)
CREATE TABLE order_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '业务唯一键',
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0:TRYING, 1:SUCCESS, 2:CANCELED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 订单明细表 (无 xid/branch_id 字段)
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL COMMENT '关联主表业务键',
    sku_id BIGINT NOT NULL,
    quantity INT,
    price DECIMAL(10, 2),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0:TRYING, 1:SUCCESS, 2:CANCELED',
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Seata TCC Fence 日志表 (必须创建，用于 useTccFence=true)
CREATE TABLE IF NOT EXISTS `tcc_fence_log`
(
    `xid`           VARCHAR(128)  NOT NULL COMMENT 'global id',
    `branch_id`     BIGINT        NOT NULL COMMENT 'branch id',
    `action_name`   VARCHAR(64)   NOT NULL COMMENT 'action name',
    `status`        TINYINT       NOT NULL COMMENT 'status(tried:1;committed:2;rollbacked:3;suspended:4)',
    `gmt_create`    DATETIME(3)   NOT NULL COMMENT 'create time',
    `gmt_modified`  DATETIME(3)   NOT NULL COMMENT 'update time',
    PRIMARY KEY (`xid`, `branch_id`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

### 2. 核心 TCC Action 实现



### 3. Mapper SQL 示例

配合上述代码，Mapper 中的 SQL 必须严格遵循状态机过滤原则：

```xml
<!-- OrderInfoMapper.xml -->

<!-- 检查订单是否存在 -->
<select id="existsByOrderNo" resultType="boolean">
    SELECT COUNT(1) > 0 FROM order_info WHERE order_no = #{orderNo}
</select>

<!-- 批量更新状态：核心在于 WHERE 条件必须包含 oldStatus -->
<update id="updateStatusByOrderNo">
    UPDATE order_info 
    SET status = #{newStatus} 
    WHERE order_no = #{orderNo} 
      AND status = #{oldStatus}
</update>
```

```xml
<!-- OrderItemMapper.xml -->

<!-- 批量更新明细状态 -->
<update id="updateStatusByOrderNo">
    UPDATE order_item 
    SET status = #{newStatus} 
    WHERE order_no = #{orderNo} 
      AND status = #{oldStatus}
</update>
```

## 六、 总结与最佳实践

1. **状态机是灵魂**：无论是 Confirm 还是 Cancel，更新 SQL 的 `WHERE` 条件中**必须包含 `status = 'TRYING'`**。这不仅保证了幂等性，也确保了只有处于中间状态的数据才会被流转。
2. **Context 传参原则**：`BusinessActionContext` 会被序列化，**千万不要传递大对象（如 List）**。只传递业务唯一键（如 `orderNo`），二阶段通过唯一键去数据库批量操作。
3. **数据源一致性**：`tcc_fence_log` 表必须和你的业务数据在**同一个数据库实例**中，以保证本地事务的原子性。
4. **版本要求**：确保 Seata Client 和 Server 版本在 1.4.2 及以上，以完美支持 `useTccFence` 特性。