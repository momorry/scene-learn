package com.moli.scene.learn.common.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long skuId;

    private Integer quantity;

    private BigDecimal price;

    private Integer status;
}
