-- Drop the old processed_events table
DROP TABLE IF EXISTS processed_events;

-- Create new universal processed_events table with composite primary key
CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- API or EVENT
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    result_data TEXT,
    PRIMARY KEY (event_id, event_type)
);

-- Create indexes for performance
CREATE INDEX idx_processed_events_type ON processed_events(event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
CREATE INDEX idx_processed_events_expires_at ON processed_events(expires_at);