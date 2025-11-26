
CREATE SEQUENCE event_store_id_seq START WITH 100 INCREMENT BY 50;

-- Create event store table for persisting all feature-related events
CREATE TABLE event_store (
    id BIGINT NOT NULL DEFAULT nextval('event_store_id_seq') PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_data TEXT NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

-- Create indexes for efficient querying
CREATE INDEX idx_event_store_code ON event_store(code);
CREATE INDEX idx_event_store_aggregate_type ON event_store(aggregate_type);
CREATE INDEX idx_event_store_event_type ON event_store(event_type);
CREATE INDEX idx_event_store_created_at ON event_store(created_at);
CREATE INDEX idx_event_store_code_version ON event_store(code, version);
CREATE INDEX idx_event_store_aggregate_type_created_at ON event_store(aggregate_type, created_at);

-- Create unique constraint on code and version to ensure version consistency
CREATE UNIQUE INDEX idx_event_store_code_version_unique ON event_store(code, version);