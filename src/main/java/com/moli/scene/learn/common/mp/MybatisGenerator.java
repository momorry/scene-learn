package com.moli.scene.learn.common.mp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.IFill;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;
import java.util.List;

/**
 * MyBatis-Plus 代码生成器
 *
 * @author moli
 */
public class MybatisGenerator {

    // 生成指定表，如果为空则全库的表
    private static final String[] INCLUDE_TABLES = new String[]{
            "t_usr"
    };

    public static void main(String[] args) {
        String absPath = System.getProperty("user.dir");
        String basePath = absPath + "/src/main/java";
        String resBasePath = absPath + "/src/main";

        FastAutoGenerator.create(
                        "jdbc:mysql://1/1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true",
                        "1",
                        "1"
                )
                // 数据源配置 - 自定义类型转换
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                    if (typeCode == java.sql.Types.TINYINT || typeCode == java.sql.Types.BIT) {
                        return DbColumnType.INTEGER;
                    }
                    if (typeCode == java.sql.Types.DOUBLE) {
                        return DbColumnType.BIG_DECIMAL;
                    }
                    return typeRegistry.getColumnType(metaInfo);
                }))
                // 全局配置
                .globalConfig(builder -> builder
                        .author("system")
                        .outputDir(basePath)
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                )
                // 包配置
                .packageConfig(builder -> builder
                        .parent("com.moli.scene.learn.common.dao")
                        .controller(null)
                        .entity("entity")
                        .mapper("mapper")
                        .service("manager")
                        .serviceImpl("manager.impl")
                        .xml("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, resBasePath + "/resources/mapper"))
                )
                // 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(INCLUDE_TABLES);

                    // 实体策略
                    builder.entityBuilder()
                            .enableLombok()
                            .enableFileOverride()
                            .addTableFills(getTableFills());

                    // Mapper 策略
                    builder.mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .enableFileOverride();

                    // Service 策略
                    builder.serviceBuilder()
                            .superServiceClass("com.baomidou.mybatisplus.extension.service.IService")
                            .superServiceImplClass("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl")
                            .formatServiceFileName("%sManager")
                            .formatServiceImplFileName("%sManagerImpl")
                            .enableFileOverride();
                })
                // 模板配置 - 使用自定义 mapper 模板
                .templateConfig(builder -> builder
                        .mapper("templates/mapper.java.vm")
                )
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("代码生成完成，项目路径: " + absPath);
    }

    /**
     * 自动填充字段配置
     */
    private static List<IFill> getTableFills() {
        return List.of(
                new Column("deleted", FieldFill.INSERT),
                new Column("create_by", FieldFill.INSERT),
                new Column("create_time", FieldFill.INSERT),
                new Column("update_by", FieldFill.INSERT_UPDATE),
                new Column("update_time", FieldFill.INSERT_UPDATE)
        );
    }
}
