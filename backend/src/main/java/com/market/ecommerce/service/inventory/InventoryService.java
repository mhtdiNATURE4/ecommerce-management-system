package com.market.ecommerce.service.inventory;

public interface InventoryService {

    /**
     * Atomically decrement stock for a single product if sufficient stock exists.
     *
     * @param productId id of the product
     * @param qty quantity to decrement (must be > 0)
     * @return true if stock was decremented (success), false otherwise
     */
    boolean decrementIfAvailable(Long productId, int qty);

    /**
     * Atomically increment stock for a single product.
     *
     * @param productId id of the product
     * @param qty quantity to increment (must be > 0)
     * @return number of rows affected (should be 1 when product exists)
     */
    int increment(Long productId, int qty);

    /**
     * Read-only: returns current stock for product (from DB).
     *
     * @param productId id of the product
     * @return current stock (non-negative)
     */
    int getStock(Long productId);

    /**
     * Admin operation: set absolute stock (for manual correction).
     *
     * @param productId id of product
     * @param newStock new stock (>= 0)
     * @return number of rows affected
     */
    int adjustAbsolute(Long productId, int newStock);
}
