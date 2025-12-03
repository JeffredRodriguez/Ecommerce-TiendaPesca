package com.tiendapesca.APItiendapesca.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tiendapesca.APItiendapesca.Entities.Product;

@Repository
public interface Product_Repository extends JpaRepository<Product, Integer> {
    
    Page<Product> findByCategorie_NameIn(List<String> categories, Pageable pageable);

    @Query("SELECT DISTINCT(p.categorie.name) FROM Product p WHERE p.stock > 0")
    List<String> findDistinctCategory();

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> searchByName(String name, Pageable pageable);
    
    Page<Product> findAll(Pageable pageable);

    // MÉTODOS CORREGIDOS PARA PRODUCTOS DESTACADOS:
    
    /**
     * Productos recientes con filtro de fecha Y límite
     */
    @Query("SELECT p FROM Product p WHERE p.date >= :cutoffDate ORDER BY p.date DESC")
    List<Product> findRecentProducts(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    
    /**
     * Productos más recientes por fecha (con paginación)
     */
    Page<Product> findAllByOrderByDateDesc(Pageable pageable);
    
    /**
     * Productos más nuevos por ID (más confiable)
     */
    Page<Product> findAllByOrderByIdDesc(Pageable pageable);
    
    /**
     * Método nativo con límite (alternativa)
     */
    @Query(value = "SELECT * FROM product ORDER BY date DESC LIMIT :limit", nativeQuery = true)
    List<Product> findTopNProducts(@Param("limit") int limit);
}