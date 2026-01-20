CREATE SEQUENCE feature_dependency_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE feature_dependencies
(
    id                       BIGINT                   NOT NULL,
    feature_code             VARCHAR(50)              NOT NULL,
    depends_on_feature_code  VARCHAR(50)              NOT NULL,
    dependency_type          VARCHAR(20)              NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes                    VARCHAR(1000),
    CONSTRAINT pk_feature_dependencies PRIMARY KEY (id)
);

ALTER TABLE feature_dependencies
    ADD CONSTRAINT FK_FEATURE_DEPENDENCIES_ON_FEATURE_CODE FOREIGN KEY (feature_code) REFERENCES features (code) ON DELETE CASCADE;

ALTER TABLE feature_dependencies
    ADD CONSTRAINT FK_FEATURE_DEPENDENCIES_ON_DEPENDS_ON_FEATURE_CODE FOREIGN KEY (depends_on_feature_code) REFERENCES features (code) ON DELETE CASCADE;

ALTER TABLE feature_dependencies
    ADD CONSTRAINT uc_feature_dependencies_feature_depends_on UNIQUE (feature_code, depends_on_feature_code);

ALTER TABLE feature_dependencies
    ADD CONSTRAINT check_no_self_dependency CHECK (feature_code != depends_on_feature_code);

ALTER TABLE feature_dependencies
    ADD CONSTRAINT check_dependency_type CHECK (dependency_type IN ('HARD', 'SOFT', 'OPTIONAL'));