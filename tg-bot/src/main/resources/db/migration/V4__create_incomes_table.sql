CREATE TABLE incomes (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(255),
    amount DOUBLE PRECISION,
    date TIMESTAMP,
    description TEXT,
    category VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(chat_id)
);
