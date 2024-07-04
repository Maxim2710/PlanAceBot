CREATE TABLE tasks (
    id BIGINT PRIMARY KEY,
    user_chat_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    completed BOOLEAN,
    creation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    priority INT,
    deadline TIMESTAMP,
    notified_three_days BOOLEAN DEFAULT FALSE,
    notified_one_day BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_chat_id) REFERENCES users(chat_id)
);
