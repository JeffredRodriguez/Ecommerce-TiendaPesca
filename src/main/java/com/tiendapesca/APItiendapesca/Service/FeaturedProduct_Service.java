package com.tiendapesca.APItiendapesca.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tiendapesca.APItiendapesca.Entities.FeaturedProduct;
import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Repository.FeaturedProduct_Repository;
import com.tiendapesca.APItiendapesca.Repository.Product_Repository;

@Service
@Transactional
public class FeaturedProduct_Service {

    private static final int MAX_FEATURED_PRODUCTS = 30;
    
    @Autowired
    private Product_Repository productRepository;
    
    @Autowired
    private FeaturedProduct_Repository featuredProductRepository;
    
  
        @Transactional
        public void refreshFeaturedProducts() {
            try {
                System.out.println("🔄 Iniciando actualización de productos destacados...");
                
                // 1. SOLO productos de los últimos 30 días
                LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
                Pageable pageable = PageRequest.of(0, MAX_FEATURED_PRODUCTS);
                
                List<Product> recentProducts = productRepository.findRecentProducts(oneMonthAgo, pageable);
                
                // 2. Log para debug
                System.out.println("📦 Productos recientes encontrados: " + recentProducts.size());
                for (Product p : recentProducts) {
                    System.out.println("   - ID: " + p.getId() + ", Fecha: " + p.getDate() + ", Nombre: " + p.getName());
                }
                
                // 3. Limpiar tabla
                featuredProductRepository.deleteAllInBatch();
                System.out.println("🗑️ Tabla featured_product limpiada");
                
                // 4. Agregar SOLO los productos recientes
                List<FeaturedProduct> featuredProducts = new ArrayList<>();
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = LocalDate.now().plusDays(30);
                
                for (Product product : recentProducts) {
                    FeaturedProduct featured = new FeaturedProduct(product, startDate, endDate);
                    featuredProducts.add(featured);
                }
                
                // 5. Guardar
                featuredProductRepository.saveAll(featuredProducts);
                
                System.out.println("🎯 Actualización completada. Productos destacados: " + featuredProducts.size());
                
            } catch (Exception e) {
                System.err.println("❌ ERROR: " + e.getMessage());
                throw new RuntimeException("Error actualizando productos destacados: " + e.getMessage(), e);
            }
        }
        
        // ... getFeaturedProducts() igual
    
    /**
     * Obtener productos destacados (ordenados por ID descendente)
     */
    public List<Product> getFeaturedProducts() {
        try {
            List<FeaturedProduct> featured = featuredProductRepository.findAllByOrderByIdDesc();
            List<Product> products = new ArrayList<>();
            
            for (FeaturedProduct fp : featured) {
                products.add(fp.getProduct());
            }
            
            return products;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}