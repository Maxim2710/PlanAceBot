CREATE TABLE users (
    chatId BIGINT PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    username VARCHAR(255),
    registeredAt TIMESTAMP
);
