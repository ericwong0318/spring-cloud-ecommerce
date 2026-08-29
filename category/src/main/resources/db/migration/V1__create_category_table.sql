CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);