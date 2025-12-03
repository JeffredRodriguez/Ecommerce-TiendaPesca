package com.tiendapesca.APItiendapesca.Repository;

import com.tiendapesca.APItiendapesca.Entities.OrderStatus;
import com.tiendapesca.APItiendapesca.Entities.Orders;
import com.tiendapesca.APItiendapesca.Entities.Users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con órdenes de compra
 * Extiende JpaRepository para operaciones CRUD básicas
 */
@Repository
public interface Orders_Repository extends JpaRepository<Orders, Integer> {
    
    /**
     * Busca todas las órdenes de un usuario específico
     * @param user Usuario dueño de las órdenes
     * @return Lista de órdenes del usuario
     */
    List<Orders> findByUser(Users user);
    
    /**
     * Busca órdenes de un usuario específico con un estado particular
     * @param user Usuario dueño de las órdenes
     * @param status Estado de la orden a filtrar
     * @return Lista de órdenes del usuario con el estado especificado
     */
    List<Orders> findByUserAndStatus(Users user, OrderStatus status);
}