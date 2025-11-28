-- Add release_owner column to releases table
ALTER TABLE releases ADD COLUMN IF NOT EXISTS release_owner VARCHAR(255);

-- Set default values for existing records
UPDATE releases SET release_owner = 'admin' WHERE release_owner IS NULL;