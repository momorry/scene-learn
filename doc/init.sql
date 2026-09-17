drop table t_usr;
create table t_usr
(
    id              bigint      not null auto_increment primary key comment 'ID',
    user_name       varchar(64) not null comment '用户名称',
    user_login_name varchar(64) not null comment '用户登录号',
    deleted tinyint(1) not null default 0 comment '是否删除 是-主键 0-否',
    create_time     datetime    null comment '创建时间',
    create_by       varchar(32) null comment '创建人',
    update_time     datetime    null comment '更新时间',
    update_by       varchar(32) null comment '更新人'
) comment '用户表';

INSERT INTO t_usr (id, user_name, user_login_name, deleted, create_time, create_by, update_time, update_by) VALUES (1, '李四', 'lisi', 0, '2026-09-14 16:47:01', 'admin', null, null);

create table t_product
(
    id              bigint      not null auto_increment primary key comment 'ID',
    product_name    varchar(64) not null comment '用户名称',
    inventory_count int         not null comment '库存数量',
    deleted         tinyint(1)  not null default 0 comment '是否删除 是-主键 0-否',
    create_time     datetime    null comment '创建时间',
    create_by       varchar(32) null comment '创建人',
    update_time     datetime    null comment '更新时间',
    update_by       varchar(32) null comment '更新人'
) comment '商品表';


-- 订单主表
CREATE TABLE order_info (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_no VARCHAR(64) NOT NULL UNIQUE, -- 业务唯一键
                            user_id BIGINT NOT NULL,
                            total_amount DECIMAL(10, 2),
                            status TINYINT NOT NULL DEFAULT 0, -- 0: TRYING, 1: SUCCESS, 2: CANCELED
                            create_time DATETIME
);

-- 订单明细表
CREATE TABLE order_item (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_no VARCHAR(64) NOT NULL, -- 关联主表
                            sku_id BIGINT NOT NULL,
                            quantity INT,
                            price DECIMAL(10, 2),
                            status TINYINT NOT NULL DEFAULT 0, -- 0: TRYING, 1: SUCCESS, 2: CANCELED
                            INDEX idx_order_no (order_no)
);