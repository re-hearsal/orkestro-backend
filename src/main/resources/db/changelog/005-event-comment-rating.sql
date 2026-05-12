ALTER TABLE event_comment ADD COLUMN IF NOT EXISTS rating INT CHECK (rating >= 0 AND rating <= 10);
