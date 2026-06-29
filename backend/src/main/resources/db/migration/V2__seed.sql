-- V2: seed categories and products

INSERT INTO categories (name) VALUES ('Electronics') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO categories (name) VALUES ('Books') ON DUPLICATE KEY UPDATE name=name;

-- =========================
-- 5 Electronic Products
-- =========================
INSERT INTO products
(name, description, price, stock, image_url, category_id)
VALUES
('iPhone 15 Pro',
 'Apple smartphone with A17 Pro chip and 256GB storage',
 1199.99,
 25,
 '/images/products/iphone15pro.png',
 (SELECT id FROM categories WHERE name = 'Electronics')),

('Samsung Galaxy S24',
 'Samsung flagship smartphone with AMOLED display',
 999.99,
 30,
 '/images/products/galaxys24.png',
 (SELECT id FROM categories WHERE name = 'Electronics')),

('Sony WH-1000XM5',
 'Noise cancelling wireless headphones',
 399.99,
 40,
 '/images/products/sonyxm5.png',
 (SELECT id FROM categories WHERE name = 'Electronics')),

('Dell XPS 15',
 'High-performance laptop with Intel Core i7',
 1799.99,
 15,
 '/images/products/dellxps15.png',
 (SELECT id FROM categories WHERE name = 'Electronics')),

('Apple Watch Series 9',
 'Smartwatch with health and fitness tracking',
 499.99,
 20,
 '/images/products/applewatch9.png',
 (SELECT id FROM categories WHERE name = 'Electronics'));

-- =========================
-- 5 Book Products
-- =========================
INSERT INTO products
(name, description, price, stock, image_url, category_id)
VALUES
('Clean Code',
 'A handbook of agile software craftsmanship by Robert C. Martin',
 39.99,
 50,
 '/images/products/cleancode.png',
 (SELECT id FROM categories WHERE name = 'Books')),

('Effective Java',
 'Best practices for Java programming by Joshua Bloch',
 45.99,
 35,
 '/images/products/effectivejava.png',
 (SELECT id FROM categories WHERE name = 'Books')),

('Spring in Action',
 'Comprehensive guide to Spring Framework',
 49.99,
 28,
 '/images/products/springinaction.png',
 (SELECT id FROM categories WHERE name = 'Books')),

('Design Patterns',
 'Elements of reusable object-oriented software',
 54.99,
 18,
 '/images/products/designpatterns.png',
 (SELECT id FROM categories WHERE name = 'Books')),

('The Pragmatic Programmer',
 'Journey to mastery for modern software developers',
 42.99,
 40,
 '/images/products/pragmaticprogrammer.png',
 (SELECT id FROM categories WHERE name = 'Books'));