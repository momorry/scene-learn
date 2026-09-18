package com.moli.scene.learn.service.idempotent;

import com.moli.scene.learn.common.dao.entity.OrderInfo;
import com.moli.scene.learn.common.domain.OrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderService {
    public OrderInfo placeOrder(OrderVo dto) {
        return null;
    }
}
