CREATE TABLE budgets (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    amount DOUBLE PRECISION,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    category VARCHAR(255),
    warning_threshold DOUBLE PRECISION,
    FOREIGN KEY (user_id) REFERENCES users(chat_id)
);
