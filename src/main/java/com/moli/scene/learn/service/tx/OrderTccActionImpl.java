package com.moli.scene.learn.service.tx;

import com.moli.scene.learn.common.dao.entity.OrderInfo;
import com.moli.scene.learn.common.dao.entity.OrderItem;
import com.moli.scene.learn.common.dao.mapper.OrderInfoMapper;
import com.moli.scene.learn.common.dao.mapper.OrderItemMapper;
import com.moli.scene.learn.common.domain.OrderItemDTO;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 TCC 接口实现 (useTccFence=true, 业务表无 xid/branch_id)
 */
@Service
@LocalTCC
public class OrderTccActionImpl implements OrderTccAction {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * Try 阶段：插入中间状态数据
     */
    @Override
    @TwoPhaseBusinessAction(
            name = "orderTccAction",
            commitMethod = "confirm",
            rollbackMethod = "cancel",
            useTCCFence = true // 开启围栏，自动处理空回滚、悬挂、幂等
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean tryCreateOrder(BusinessActionContext context,
                                  @BusinessActionContextParameter(paramName = "orderNo") String orderNo,
                                  @BusinessActionContextParameter(paramName = "userId") Long userId,
                                  @BusinessActionContextParameter(paramName = "items") List<OrderItemDTO> items) {

        // 1. 业务幂等检查：防止同一个 orderNo 被重复 Try
        // 注意：这里依靠数据库 order_no 的唯一索引来保证最终一致性，代码层检查只是优化
        if (orderInfoMapper.existsByOrderNo(orderNo)) {
            return true;
        }

        // 2. 插入订单主表 (状态为 TRYING)
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setUserId(userId);
        orderInfo.setStatus(OrderStatus.TRYING.getCode()); // 0: TRYING
        // ... 设置其他业务字段，如总金额
        orderInfoMapper.insert(orderInfo);

        // 3. 插入订单明细表 (多行，状态为 TRYING)
        for (OrderItemDTO item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNo(orderNo); // 关键：通过 orderNo 关联
            orderItem.setSkuId(item.getSkuId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            orderItem.setStatus(OrderStatus.TRYING.getCode()); // 0: TRYING
            orderItemMapper.insert(orderItem);
        }

        return true;
    }

    /**
     * Confirm 阶段：将中间状态转为成功状态
     * <p>
     * 关键点：
     * 1. Seata Fence 保证了该方法不会被重复调用（幂等）。
     * 2. 由于业务表没有 xid，我们只能通过 orderNo 来定位数据。
     * 3. 必须加上 AND status = TRYING，防止误操作已经 Cancel 的数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext context) {
        Map<String, Object> params = context.getActionContext();
        String orderNo = (String) params.get("orderNo");

        // 【修正】不再使用 xid，而是使用 orderNo
        int orderCount = orderInfoMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.SUCCESS.getCode(),
                OrderStatus.TRYING.getCode()
        );

        int itemCount = orderItemMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.SUCCESS.getCode(),
                OrderStatus.TRYING.getCode()
        );

        // 可选：校验更新行数是否符合预期
        return true;
    }

    /**
     * Cancel 阶段：将中间状态转为取消状态
     * <p>
     * 关键点：
     * 1. 如果是空回滚（Try 未执行），Seata Fence 会直接返回成功，不会进入此方法。
     * 2. 同样通过 orderNo 定位数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext context) {
        Map<String, Object> params = context.getActionContext();
        String orderNo = (String) params.get("orderNo");

        // 【修正】不再使用 xid，而是使用 orderNo
        orderInfoMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.CANCELED.getCode(),
                OrderStatus.TRYING.getCode()
        );

        orderItemMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.CANCELED.getCode(),
                OrderStatus.TRYING.getCode()
        );

        return true;
    }
}