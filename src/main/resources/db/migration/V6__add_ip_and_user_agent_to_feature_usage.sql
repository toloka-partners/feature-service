alter table feature_usage add column ip_address varchar(50);
alter table feature_usage add column user_agent text;

create index idx_feature_usage_ip_address on feature_usage (ip_address);

