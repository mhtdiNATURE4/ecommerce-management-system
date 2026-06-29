-- V7__fix_null_versions.sql
-- Fix null version values and make version NOT NULL with default 0.

-- products
SET @product_version_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'products'
    AND COLUMN_NAME = 'version'
);
SET @product_sql = IF(
  @product_version_exists = 0,
  'ALTER TABLE products ADD COLUMN `version` BIGINT DEFAULT 0',
  'SELECT 1'
);
PREPARE product_stmt FROM @product_sql;
EXECUTE product_stmt;
DEALLOCATE PREPARE product_stmt;
UPDATE products SET `version` = 0 WHERE `version` IS NULL;
ALTER TABLE products MODIFY `version` BIGINT NOT NULL DEFAULT 0;

-- orders
SET @order_version_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'version'
);
SET @order_sql = IF(
  @order_version_exists = 0,
  'ALTER TABLE orders ADD COLUMN `version` BIGINT DEFAULT 0',
  'SELECT 1'
);
PREPARE order_stmt FROM @order_sql;
EXECUTE order_stmt;
DEALLOCATE PREPARE order_stmt;
UPDATE orders SET `version` = 0 WHERE `version` IS NULL;
ALTER TABLE orders MODIFY `version` BIGINT NOT NULL DEFAULT 0;
