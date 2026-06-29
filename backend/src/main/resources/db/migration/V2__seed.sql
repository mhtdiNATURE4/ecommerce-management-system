-- V2: seed categories and products

INSERT INTO categories (name) VALUES ('Electronics') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO categories (name) VALUES ('Books') ON DUPLICATE KEY UPDATE name=name;

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Mechanical Keyboard', 'Premium mechanical keyboard with tactile switches for fast, precise typing.', 129.99, 48, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Electronics'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Bluetooth Headphones', 'Over-ear wireless headphones with immersive sound and all-day comfort.', 149.50, 62, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Electronics'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('USB-C Hub', 'Compact multi-port hub with HDMI, USB 3.0, and power delivery support.', 79.99, 85, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Electronics'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Portable SSD (1TB)', 'Fast and reliable portable SSD designed for file backups and on-the-go productivity.', 109.99, 37, 'https://images.unsplash.com/photo-1587202372775-e229f172b8d1?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Electronics'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Full HD Webcam', 'High-definition webcam with built-in microphone for video calls and streaming.', 89.00, 54, 'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Electronics'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('The Pragmatic Programmer', 'A classic guide to practical software development and engineering excellence.', 45.50, 29, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Books'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Design Patterns', 'An essential reference for building flexible and maintainable object-oriented software.', 39.99, 73, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Books'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Introduction to Algorithms', 'Comprehensive coverage of fundamental algorithms and data structures for students and professionals.', 59.99, 41, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Books'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Atomic Habits', 'A practical framework for building good habits and breaking bad ones.', 24.95, 67, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Books'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);

INSERT INTO products (name, description, price, stock, image_url, category_id)
VALUES
('Deep Work', 'A focused guide to producing high-quality work in a distracted world.', 21.99, 58, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=800&q=80', (SELECT id FROM categories WHERE name='Books'))
ON DUPLICATE KEY UPDATE description=VALUES(description), price=VALUES(price), stock=VALUES(stock), image_url=VALUES(image_url), category_id=VALUES(category_id);
