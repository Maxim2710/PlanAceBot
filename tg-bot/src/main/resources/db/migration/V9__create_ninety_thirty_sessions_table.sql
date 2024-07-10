CREATE TABLE ninety_thirty_sessions (
    id BIGINT PRIMARY KEY,
    user_chat_id BIGINT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    interval_type VARCHAR(255),
    session_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_chat_id) REFERENCES users(chat_id)
);
