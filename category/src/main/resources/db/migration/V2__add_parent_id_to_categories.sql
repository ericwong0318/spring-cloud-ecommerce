ALTER TABLE categories ADD COLUMN parent_id BIGINT;
ALTER TABLE categories ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);