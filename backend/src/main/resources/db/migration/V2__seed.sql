-- V2: seed categories only

INSERT INTO categories (name) VALUES ('Electronics') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO categories (name) VALUES ('Books') ON DUPLICATE KEY UPDATE name=name;

-- =========================
-- 5 Electronic Products
-- =========================
INSERT INTO products
(name, description, price, stock, image_url, category_id, version)
VALUES
('iPhone 15 Pro',
 'Apple smartphone with A17 Pro chip and 256GB storage',
 1199.99,
 25,
 '/images/products/iphone15pro.jpg',
 1,
 0),

('Samsung Galaxy S24',
 'Samsung flagship smartphone with AMOLED display',
 999.99,
 30,
 '/images/products/galaxys24.jpg',
 1,
 0),

('Sony WH-1000XM5',
 'Noise cancelling wireless headphones',
 399.99,
 40,
 '/images/products/sonyxm5.jpg',
 1,
 0),

('Dell XPS 15',
 'High-performance laptop with Intel Core i7',
 1799.99,
 15,
 '/images/products/dellxps15.jpg',
 1,
 0),

('Apple Watch Series 9',
 'Smartwatch with health and fitness tracking',
 499.99,
 20,
 '/images/products/applewatch9.jpg',
 1,
 0);

-- =========================
-- 5 Book Products
-- =========================
INSERT INTO products
(name, description, price, stock, image_url, category_id, version)
VALUES
('Clean Code',
 'A handbook of agile software craftsmanship by Robert C. Martin',
 39.99,
 50,
 '/images/products/cleancode.jpg',
 2,
 0),

('Effective Java',
 'Best practices for Java programming by Joshua Bloch',
 45.99,
 35,
 '/images/products/effectivejava.jpg',
 2,
 0),

('Spring in Action',
 'Comprehensive guide to Spring Framework',
 49.99,
 28,
 '/images/products/springinaction.jpg',
 2,
 0),

('Design Patterns',
 'Elements of reusable object-oriented software',
 54.99,
 18,
 '/images/products/designpatterns.jpg',
 2,
 0),

('The Pragmatic Programmer',
 'Journey to mastery for modern software developers',
 42.99,
 40,
 '/images/products/pragmaticprogrammer.jpg',
 2,
 0);