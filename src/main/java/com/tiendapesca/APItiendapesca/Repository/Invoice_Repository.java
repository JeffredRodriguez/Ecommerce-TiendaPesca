package com.tiendapesca.APItiendapesca.Repository;

import com.tiendapesca.APItiendapesca.Entities.Invoice;
import com.tiendapesca.APItiendapesca.Entities.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con facturas
 * Extiende JpaRepository para operaciones CRUD básicas
 */
@Repository
public interface Invoice_Repository extends JpaRepository<Invoice, Integer> {
    
    /**
     * Busca una factura por la orden asociada
     * @param order Orden relacionada con la factura
     * @return Optional con la factura si existe
     */
    Optional<Invoice> findByOrder(Orders order);
    
    /**
     * Busca una factura por el ID de la orden asociada
     * @param orderId ID de la orden relacionada con la factura
     * @return Optional con la factura si existe
     */
    Optional<Invoice> findByOrderId(Integer orderId);
    
    /**
     * Verifica si existe una factura para una orden específica
     * @param order Orden a verificar
     * @return true si existe una factura para la orden, false en caso contrario
     */
    boolean existsByOrder(Orders order);
}