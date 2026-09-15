package com.moli.scene.learn.common.dao.manager.impl;

import com.moli.scene.learn.common.dao.entity.TProduct;
import com.moli.scene.learn.common.dao.mapper.TProductMapper;
import com.moli.scene.learn.common.dao.manager.TProductManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author system
 * @since 2026-09-15
 */
@Service
public class TProductManagerImpl extends ServiceImpl<TProductMapper, TProduct> implements TProductManager {

}
