-- Bot users (registered users with access)
CREATE TABLE bot_user (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE NOT NULL,
    username      VARCHAR(32),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    role          VARCHAR(16) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invitations (pending users added by username)
CREATE TABLE bot_user_invitation (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(32) UNIQUE NOT NULL,
    role          VARCHAR(16) NOT NULL DEFAULT 'USER',
    created_by    BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
