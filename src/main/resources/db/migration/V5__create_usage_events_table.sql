create sequence usage_event_id_seq start with 100 increment by 50;

create table usage_events
(
    id            bigint       not null default nextval('usage_event_id_seq'),
    feature_code  varchar(255) not null,
    product_code  varchar(255) not null,
    user_id       varchar(255) not null,
    timestamp     timestamp    not null default current_timestamp,
    metadata      text,
    primary key (id)
);

create index idx_usage_events_feature_code on usage_events (feature_code);
create index idx_usage_events_product_code on usage_events (product_code);
create index idx_usage_events_timestamp on usage_events (timestamp);

