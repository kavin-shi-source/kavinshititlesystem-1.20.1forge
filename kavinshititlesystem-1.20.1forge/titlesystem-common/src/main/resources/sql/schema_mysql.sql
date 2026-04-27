CREATE TABLE IF NOT EXISTS player_titles_core (
    uuid VARCHAR(36) PRIMARY KEY,
    equipped_id INT NOT NULL DEFAULT -1,
    alive_minutes INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_unlocked_titles (
    uuid VARCHAR(36) NOT NULL,
    title_id INT NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, title_id),
    FOREIGN KEY (uuid) REFERENCES player_titles_core(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_kill_counts (
    uuid VARCHAR(36) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    kill_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid, target_type),
    FOREIGN KEY (uuid) REFERENCES player_titles_core(uuid) ON DELETE CASCADE
);
