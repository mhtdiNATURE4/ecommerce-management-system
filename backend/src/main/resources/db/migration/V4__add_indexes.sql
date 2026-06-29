-- Add indexes to improve query performance on high-frequency lookups
-- Note: some databases (MySQL) do not support IF NOT EXISTS for CREATE INDEX.
-- This migration assumes indexes are not present on a fresh schema. If you need
-- idempotent behavior, replace with a Java-based Flyway migration that checks
-- INFORMATION_SCHEMA before creating indexes.
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
CREATE INDEX idx_cart_items_user_id ON cart_items (user_id);
-- Add user index for faster user-based queries
CREATE INDEX idx_orders_user_id ON orders (user_id);
