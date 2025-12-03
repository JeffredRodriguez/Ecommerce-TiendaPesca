package com.tiendapesca.APItiendapesca.Controller;

import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Service.FeaturedProduct_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/featured-products")
public class FeaturedProduct_Controller {

    @Autowired
    private FeaturedProduct_Service featuredProductService;

    /**
     * Obtiene todos los productos destacados (los 30 más recientes)
     */
    @GetMapping
    public ResponseEntity<List<Product>> getFeaturedProducts() {
        List<Product> featuredProducts = featuredProductService.getFeaturedProducts();
        return ResponseEntity.ok(featuredProducts);
    }

    /**
     * Forzar actualización manual de productos destacados
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshFeaturedProducts() {
        featuredProductService.refreshFeaturedProducts();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Productos destacados actualizados exitosamente");
        response.put("total", featuredProductService.getFeaturedProducts().size());
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Verificar estado de los productos destacados
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFeaturedStatus() {
        List<Product> featuredProducts = featuredProductService.getFeaturedProducts();
        
        Map<String, Object> status = new HashMap<>();
        status.put("featuredCount", featuredProducts.size());
        status.put("maxLimit", 30);
        status.put("message", "Sistema de productos destacados activo");
        status.put("autoUpdate", "ACTIVO - Se actualiza automáticamente al crear productos");
        
        return ResponseEntity.ok(status);
    }
}