package com.moli.scene.learn.common.dao.manager.impl;

import com.moli.scene.learn.common.dao.entity.OrderItem;
import com.moli.scene.learn.common.dao.mapper.OrderItemMapper;
import com.moli.scene.learn.common.dao.manager.OrderItemManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author system
 * @since 2026-09-17
 */
@Service
public class OrderItemManagerImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemManager {

}
