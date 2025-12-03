package com.tiendapesca.APItiendapesca.Events;

import com.tiendapesca.APItiendapesca.Entities.Product;

public class ProductCreatedEvent {
    private final Product product;
    
    public ProductCreatedEvent(Product product) {
        this.product = product;
    }
    
    public Product getProduct() {
        return product;
    }
}