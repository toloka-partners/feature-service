create sequence usage_event_id_seq start with 100 increment by 50;

create table usage_events
(
    id            bigint       not null default nextval('usage_event_id_seq'),
    feature_code  varchar(50)  not null,
    product_code  varchar(50)  not null,
    user_id       varchar(255) not null,
    event_type    varchar(50)  not null,
    metadata      text,
    created_at    timestamp    not null default current_timestamp,
    primary key (id)
);

create index idx_usage_events_feature_code on usage_events(feature_code);
create index idx_usage_events_product_code on usage_events(product_code);
create index idx_usage_events_user_id on usage_events(user_id);
create index idx_usage_events_created_at on usage_events(created_at);
create index idx_usage_events_event_type on usage_events(event_type);

