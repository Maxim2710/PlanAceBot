CREATE TABLE users (
    chat_id BIGINT PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    username VARCHAR(255),
    registered_at TIMESTAMP,
    timezone VARCHAR(255))
);
