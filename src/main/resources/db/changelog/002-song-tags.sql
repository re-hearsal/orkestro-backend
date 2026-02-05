
CREATE TABLE song_tags (
    song_id BIGINT NOT NULL,
    tag TEXT NOT NULL,
    PRIMARY KEY (song_id, tag),
    FOREIGN KEY (song_id) REFERENCES song (id) ON DELETE CASCADE
);

CREATE INDEX idx_song_tags_tag ON song_tags (tag);

