package com.moli.scene.learn.service.tx;

import com.moli.scene.learn.common.dao.entity.TProduct;
import com.moli.scene.learn.common.dao.mapper.TProductMapper;
import com.moli.scene.learn.common.dao.mapper.TUsrMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAtService {

    private final TProductMapper tProductMapper;
    private final TUsrMapper tUsrMapper;
    /**
     * AT模式
     * 只是个案例，代码无业务逻辑
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    public void addProduct() {

        TProduct t = new TProduct();
        tProductMapper.insert(t);
        //远程调用其他服务
        //orderService.add(t);
    }



}
