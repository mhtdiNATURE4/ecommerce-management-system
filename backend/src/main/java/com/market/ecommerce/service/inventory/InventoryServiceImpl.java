package com.market.ecommerce.service.inventory;

import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    public InventoryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public boolean decrementIfAvailable(Long productId, int qty) {
        if (qty <= 0) return false;
        // Use pessimistic lock to read-modify-write product row and update the version correctly
        var opt = productRepository.findByIdForUpdate(productId);
        if (opt.isEmpty()) throw new ResourceNotFoundException("المنتج غير موجود");
        var p = opt.get();
        if (p.getStock() < qty) return false;
        p.setStock(p.getStock() - qty);
        productRepository.save(p);
        return true;
    }

    @Override
    @Transactional
    public int increment(Long productId, int qty) {
        if (qty <= 0) return 0;
        var opt = productRepository.findByIdForUpdate(productId);
        if (opt.isEmpty()) throw new ResourceNotFoundException("المنتج غير موجود");
        var p = opt.get();
        p.setStock(p.getStock() + qty);
        productRepository.save(p);
        return 1;
    }

    @Override
    public int getStock(Long productId) {
        Integer s = productRepository.findStockById(productId);
        if (s == null) throw new ResourceNotFoundException("المنتج غير موجود");
        return s;
    }

    @Override
    @Transactional
    public int adjustAbsolute(Long productId, int newStock) {
        // implement as simple fetch+save to set absolute value
        var opt = productRepository.findById(productId);
        if (opt.isEmpty()) throw new ResourceNotFoundException("المنتج غير موجود");
        var p = opt.get();
        p.setStock(newStock);
        productRepository.save(p);
        return 1;
    }
}
