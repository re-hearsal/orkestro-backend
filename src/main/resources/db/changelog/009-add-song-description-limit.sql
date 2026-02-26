ALTER TABLE song
ADD CONSTRAINT chk_song_description_length
CHECK (description IS NULL OR char_length(description) <= 3000);
