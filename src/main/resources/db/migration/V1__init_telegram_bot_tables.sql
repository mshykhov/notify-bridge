-- Chain state (dialog steps)
CREATE TABLE chain (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id BIGINT NOT NULL,
    from_id BIGINT NOT NULL,
    step VARCHAR(255),
    content TEXT
);

-- Callback content storage (for data > 64 bytes)
CREATE TABLE callback_content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id BIGINT NOT NULL,
    from_id BIGINT NOT NULL,
    callback_id UUID NOT NULL,
    content TEXT NOT NULL,
    update_date TIMESTAMP NOT NULL
);

-- Telegram users
CREATE TABLE telegram_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    language_code VARCHAR(10),
    available BOOLEAN NOT NULL,
    update_date TIMESTAMP NOT NULL,
    create_date TIMESTAMP NOT NULL
);

-- Telegram chats
CREATE TABLE telegram_chat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id BIGINT NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    available BOOLEAN NOT NULL,
    update_date TIMESTAMP NOT NULL,
    create_date TIMESTAMP NOT NULL
);

-- Chat status events
CREATE TABLE telegram_chat_status_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id BIGINT NOT NULL,
    title VARCHAR(255),
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_chain_chat_from ON chain(chat_id, from_id);
CREATE INDEX idx_callback_content_chat_from ON callback_content(chat_id, from_id);
CREATE INDEX idx_telegram_chat_status_event_chat ON telegram_chat_status_event(chat_id);
