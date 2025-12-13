-- Create user_pushover_config table
CREATE TABLE user_pushover_config (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT UNIQUE NOT NULL REFERENCES bot_user(id) ON DELETE CASCADE,
    user_key         VARCHAR(30) NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    default_priority INTEGER NOT NULL DEFAULT 0,
    sound            VARCHAR(50),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Migrate existing data from bot_user
INSERT INTO user_pushover_config (user_id, user_key)
SELECT id, pushover_user_key
FROM bot_user
WHERE pushover_user_key IS NOT NULL;

-- Remove old column from bot_user
ALTER TABLE bot_user DROP COLUMN pushover_user_key;
