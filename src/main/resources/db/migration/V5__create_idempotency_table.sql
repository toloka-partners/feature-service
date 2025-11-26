CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    operation_type VARCHAR(100) NOT NULL,
    result VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_idempotency_created_at ON idempotency_keys(created_at);