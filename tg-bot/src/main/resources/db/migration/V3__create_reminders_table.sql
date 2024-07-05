CREATE TABLE reminders (
    id SERIAL PRIMARY KEY,
    user_chat_id BIGINT NOT NULL,
    reminder_time TIMESTAMP NOT NULL,
    message TEXT NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_chat_id) REFERENCES users(chat_id)
);
