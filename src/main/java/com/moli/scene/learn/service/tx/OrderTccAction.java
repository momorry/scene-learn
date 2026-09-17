package com.moli.scene.learn.service.tx;

import com.moli.scene.learn.common.domain.OrderItemDTO;
import io.seata.rm.tcc.api.BusinessActionContext;

import java.util.List;

public interface OrderTccAction {

    boolean tryCreateOrder(BusinessActionContext context,
                                  String orderNo,
                                  Long userId,
                                  List<OrderItemDTO> items);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

