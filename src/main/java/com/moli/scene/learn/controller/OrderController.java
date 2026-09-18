package com.moli.scene.learn.controller;

import com.moli.scene.learn.common.base.ApiResult;
import com.moli.scene.learn.common.dao.entity.OrderInfo;
import com.moli.scene.learn.common.domain.OrderVo;
import com.moli.scene.learn.service.idempotent.IdempotentHelper;
import com.moli.scene.learn.service.idempotent.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private IdempotentHelper idempotentHelper;

    @PostMapping("/order/create")
    public ApiResult<OrderInfo> createOrder(@RequestBody OrderVo dto) {
        // 1. 幂等校验：requestId 由前端生成并随参数传递
        //    过期时间建议设为接口正常耗时的 2~3 倍，如 5-15 分钟
        if (!idempotentHelper.tryAcquire(dto.getRequestId(), 300)) {
            return ApiResult.fail("订单处理中，请勿重复提交");
        }

        try {
            // 2. 执行下单业务逻辑
            OrderInfo order = orderService.placeOrder(dto);
            return ApiResult.ok(order);
        } catch (Exception e) {
            // 3. 业务执行失败，释放幂等锁，允许前端重试
            idempotentHelper.release(dto.getRequestId());
            throw e;
        }
    }
}