package com.moli.scene.learn.common.dao.mapper;

import com.moli.scene.learn.common.dao.entity.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author system
 * @since 2026-09-17
 */
@Repository
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    boolean existsByOrderNo(String orderNo);

    int updateStatusByOrderNo(String orderNo, Integer newOrderStatus, Integer oldOrderStatus);
}
