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
