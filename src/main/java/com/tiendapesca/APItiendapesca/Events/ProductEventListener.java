package com.tiendapesca.APItiendapesca.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tiendapesca.APItiendapesca.Service.FeaturedProduct_Service;

import jakarta.transaction.Transactional;

@Component
public class ProductEventListener {

    @Autowired
    private FeaturedProduct_Service featuredProductService;

    @EventListener
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional // ⬅️ AGREGAR ESTO
    public void handleProductCreated(ProductCreatedEvent event) {
        System.out.println("🆕 EVENTO: Producto creado - " + event.getProduct().getName());
        
        try {
            featuredProductService.refreshFeaturedProducts();
            System.out.println("✅ Productos destacados actualizados automáticamente");
        } catch (Exception e) {
            System.err.println("❌ Error actualizando productos destacados: " + e.getMessage());
        }
    }
}