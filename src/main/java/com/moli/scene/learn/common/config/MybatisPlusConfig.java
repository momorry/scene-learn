package com.moli.scene.learn.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author moli
 */
@MapperScan("com.moli.scene.learn.common.dao.mapper")
@Configuration
public class MybatisPlusConfig {
    /**
     * 分页插件
     *
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        //每页最大条数
        paginationInnerInterceptor.setMaxLimit(5000L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        //防止全表删除，全表更新
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }


    /**
     * 重写枚举类型的toString方法
     *
     * @return
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder.featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    }
}
