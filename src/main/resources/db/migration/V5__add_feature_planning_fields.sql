alter table features
    add column planned_completion_date date,
    add column planning_status varchar(50),
    add column feature_owner varchar(255),
    add column notes text,
    add column blockage_reason varchar(500);
