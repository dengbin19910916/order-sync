-- auto-generated definition
create table error
(
    id           bigint auto_increment
        primary key,
    created_time datetime(6) null,
    schedule_id  bigint      null,
    constraint FKpyhexvj6khf1u2rnc24r38jy5
        foreign key (schedule_id) references schedule (id)
);

-- auto-generated definition
create table log
(
    id           bigint auto_increment
        primary key,
    created_time datetime(6) null,
    schedule_id  bigint      null,
    constraint FK80qgessuqts1893sv8ywe2ph0
        foreign key (schedule_id) references schedule (id)
);

-- auto-generated definition
create table property
(
    id            bigint auto_increment
        primary key,
    created_time  datetime(6)  null,
    order_channel int          null,
    order_type    int          null,
    origin_time   datetime(6)  null,
    shop_code     varchar(255) null,
    updated_time  datetime(6)  null
);

-- auto-generated definition
create table refund
(
    id           bigint auto_increment
        primary key,
    created_time datetime(6)  null,
    data         varchar(255) null,
    rid          varchar(255) null,
    tid          varchar(255) null,
    updated_time datetime(6)  null
);

-- auto-generated definition
create table schedule
(
    id                bigint auto_increment
        primary key,
    created_time      datetime(6)  null,
    end_time          datetime(6)  null,
    end_time_millis   bigint       null,
    start_time        datetime(6)  null,
    start_time_millis bigint       null,
    updated_time      datetime(6)  null,
    order_channel     int          null,
    order_type        int          null,
    shop_code         varchar(255) null
);

-- auto-generated definition
create table trade
(
    id           bigint auto_increment
        primary key,
    created_time datetime(6)  null,
    data         varchar(255) null,
    tid          varchar(255) null,
    updated_time datetime(6)  null
);



