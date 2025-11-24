-- Add feature planning fields to features table
ALTER TABLE features
    ADD COLUMN planning_status VARCHAR(50),
    ADD COLUMN planned_completion_date TIMESTAMP,
    ADD COLUMN feature_owner VARCHAR(255),
    ADD COLUMN blockage_reason VARCHAR(1000),
    ADD COLUMN planning_notes TEXT;

-- Set default planning_status for features that are assigned to releases
UPDATE features
SET planning_status = 'NOT_STARTED'
WHERE release_id IS NOT NULL AND planning_status IS NULL;
