-- Add planning fields to releases table
ALTER TABLE releases
ADD COLUMN planned_start_date timestamp,
ADD COLUMN planned_release_date timestamp,
ADD COLUMN actual_release_date timestamp,
ADD COLUMN owner varchar(255),
ADD COLUMN notes text;

-- Create indexes for better query performance
CREATE INDEX idx_releases_planned_start_date ON releases(planned_start_date);
CREATE INDEX idx_releases_planned_release_date ON releases(planned_release_date);
CREATE INDEX idx_releases_actual_release_date ON releases(actual_release_date);
CREATE INDEX idx_releases_owner ON releases(owner);
CREATE INDEX idx_releases_status ON releases(status);