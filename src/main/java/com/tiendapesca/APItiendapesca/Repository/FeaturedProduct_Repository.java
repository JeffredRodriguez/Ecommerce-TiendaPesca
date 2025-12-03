package com.tiendapesca.APItiendapesca.Repository;

import com.tiendapesca.APItiendapesca.Entities.FeaturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturedProduct_Repository extends JpaRepository<FeaturedProduct, Integer> {
    
    /**
     * Obtiene todos los productos destacados ordenados por ID descendente
     * @return Lista de productos destacados ordenados por ID (más recientes primero)
     */
    @Query("SELECT fp FROM FeaturedProduct fp ORDER BY fp.id DESC")
    List<FeaturedProduct> findAllByOrderByIdDesc();
    
    /**
     * Verifica si un producto ya está destacado
     * @param productId ID del producto a verificar
     * @return true si el producto está destacado, false en caso contrario
     */
    @Query("SELECT COUNT(fp) > 0 FROM FeaturedProduct fp WHERE fp.product.id = :productId")
    boolean existsByProductId(@Param("productId") Integer productId);
    
    /**
     * Elimina todos los productos destacados (MÁS SEGURO)
     */
    @Modifying
    @Query("DELETE FROM FeaturedProduct")
    void deleteAllInBatch();
    
    /**
     * Elimina productos destacados por IDs de producto
     * @param productIds Lista de IDs de producto a eliminar
     */
    @Modifying
    @Query("DELETE FROM FeaturedProduct fp WHERE fp.product.id IN :productIds")
    void deleteByProductIdIn(@Param("productIds") List<Integer> productIds);
    
    /**
     * Elimina productos destacados que NO están en la lista de IDs
     * @param productIds Lista de IDs de producto a mantener
     */
    @Modifying
    @Query("DELETE FROM FeaturedProduct fp WHERE fp.product.id NOT IN :productIds")
    void deleteByProductIdNotIn(@Param("productIds") List<Integer> productIds);
    
    /**
     * Cuenta la cantidad total de productos destacados
     * @return Número de productos destacados
     */
    long count();
    
    /**
     * Obtiene productos destacados activos (no vencidos)
     * @return Lista de productos destacados que aún están vigentes
     */
    @Query("SELECT fp FROM FeaturedProduct fp WHERE fp.endDate >= CURRENT_DATE ORDER BY fp.id DESC")
    List<FeaturedProduct> findActiveFeaturedProducts();
    
    /**
     * Busca un producto destacado por ID de producto
     * @param productId ID del producto
     * @return FeaturedProduct si existe, null en caso contrario
     */
    @Query("SELECT fp FROM FeaturedProduct fp WHERE fp.product.id = :productId")
    FeaturedProduct findByProductId(@Param("productId") Integer productId);
    
    /**
     * Encuentra productos destacados por rango de fechas
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de productos destacados en el rango de fechas
     */
    @Query("SELECT fp FROM FeaturedProduct fp WHERE fp.startDate <= :endDate AND fp.endDate >= :startDate ORDER BY fp.id DESC")
    List<FeaturedProduct> findFeaturedProductsByDateRange(@Param("startDate") java.time.LocalDate startDate, 
                                                         @Param("endDate") java.time.LocalDate endDate);
}