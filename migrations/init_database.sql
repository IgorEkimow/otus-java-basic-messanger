CREATE DATABASE messanger_db;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

INSERT INTO users (login, password, username, role) VALUES
    ('admin', 'admin', 'admin', 'ADMIN'),
    ('user1', 'user1', 'user1', 'USER'),
    ('user2', 'user2', 'user2', 'USER'),
    ('user3', 'user3', 'user3', 'USER')
    ON CONFLICT (login) DO NOTHING;