create database meeting;

use meeting;

create table user
(
    id            int primary key comment '用户 ID',
    email         varchar(50) not null comment '邮箱',
    nickName      varchar(20) null comment '昵称',
    sex           tinyint(1)  not null default 2 comment '性别 0:女 1:男 2:保密',
    password      varchar(32) not null comment '密码',
    status        tinyint(1)  not null default 0 comment '状态',
    meetingNo     varchar(10) comment '个人会议号',
    createTime    datetime    not null default CURRENT_TIMESTAMP comment '创建时间',
    updateTime    datetime    not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    userRole      varchar(20) not null default 'user' comment 'user/admin/ban',
    lastLoginTime bigint(20) comment '最后登录时间',
    lastOffTime   bigint(20) comment '最后离开时间',
    unique key idx_key_email (email)
) comment '用户表';

create table meeting
(
    id           int primary key comment '会议 ID',
    meetingNo    varchar(10)  not null comment '会议号',
    name         varchar(100) not null comment '会议名',
    createUserId int          not null comment '创建人 ID',
    joinType     tinyint(1)   not null comment '加入方式',
    joinPassword varchar(5) comment '加入密码',
    startTime    datetime comment '开始时间',
    endTime      datetime comment '结束时间',
    status       tinyint(1)   not null comment '状态',
    createTime   datetime     not null default CURRENT_TIMESTAMP comment '创建时间',
    updateTime   datetime     not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间'
) comment '会议表';

create table meetingMember
(
    meetingId     int not null comment '会议 ID',
    userId        int not null comment '用户 ID',
    nickName      varchar(20) comment '昵称',
    lastJoinTime  datetime comment '最后一次加入时间',
    status        tinyint(1) comment '状态',
    memberType    tinyint(1),
    meetingStatus tinyint(4) comment '会议状态',
    primary key (meetingId, userId)
) comment '会议成员表';

create table meetingReserve
(
    meetingId    int primary key comment '会议 ID',
    name         varchar(100) not null comment '会议名',
    joinType     tinyint(1)   not null comment '加入方式',
    joinPassword varchar(5) comment '加入密码',
    duration     int comment '会议时长',
    startTime    datetime comment '开始时间',
    createUserId int          not null comment '创建人 ID',
    status       tinyint(1)   not null comment '状态',
    createTime   datetime     not null default CURRENT_TIMESTAMP comment '创建时间',
    updateTime   datetime     not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间'
) comment '预约会议表';

create table meetingReserveMember
(
    meetingId    int not null comment '会议 ID',
    inviteUserId int not null comment '邀请用户 ID',
    primary key (meetingId, inviteUserId)
) comment '预约会议成员表';

create table userContactApply
(
    id            int primary key auto_increment comment '申请 ID',
    applyUserId   int        not null comment '申请人 ID',
    receiveUserId int        not null comment '接收人 ID',
    status        tinyint(1) not null comment '状态',
    lastApplyTime datetime   not null default CURRENT_TIMESTAMP comment '最后一次申请时间',
    unique key idx_key (applyUserId, receiveUserId),
    key idx_last_apply_time (lastApplyTime)
) comment '联系人申请表';

create table userContact
(
    userId         int        not null comment '用户 ID',
    contactId      int        not null comment '联系人 ID',
    status         tinyint(1) not null comment '状态',
    lastUpdateTime datetime   not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '最后一次更新时间',
    primary key (userId, contactId)
) comment '联系人表';