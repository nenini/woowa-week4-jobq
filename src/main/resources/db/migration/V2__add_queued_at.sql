ALTER TABLE job ADD COLUMN IF NOT EXISTS queued_at timestamptz;
