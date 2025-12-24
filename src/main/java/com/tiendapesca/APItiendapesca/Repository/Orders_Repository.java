package com.tiendapesca.APItiendapesca.Repository;

import com.tiendapesca.APItiendapesca.Entities.OrderStatus;
import com.tiendapesca.APItiendapesca.Entities.Orders;
import com.tiendapesca.APItiendapesca.Entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con órdenes de compra
 * Extiende JpaRepository para operaciones CRUD básicas
 */
@Repository
public interface Orders_Repository extends JpaRepository<Orders, Integer> {

    //CONSULTAS ESPECÍFICAS PARA CARGAR RELACIONES

    /**
     * Busca una orden por ID cargando todos los detalles, productos y usuario
     * @param id ID de la orden
     * @return Orden con todas las relaciones cargadas
     */
    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.id = :id")
    Optional<Orders> findByIdWithAllDetails(@Param("id") Integer id);

    /**
     * Busca una orden por ID cargando solo detalles y productos
     * @param id ID de la orden
     * @return Orden con detalles y productos cargados
     */
    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "WHERE o.id = :id")
    Optional<Orders> findByIdWithDetails(@Param("id") Integer id);

    /**
     * Busca una orden por ID cargando solo el usuario
     * @param id ID de la orden
     * @return Orden con usuario cargado
     */
    @Query("SELECT o FROM Orders o " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.id = :id")
    Optional<Orders> findByIdWithUser(@Param("id") Integer id);

    // CONSULTAS POR USUARIO

    /**
     * Busca todas las órdenes de un usuario específico
     * @param user Usuario dueño de las órdenes
     * @return Lista de órdenes del usuario
     */
    List<Orders> findByUser(Users user);

    /**
     * Busca todas las órdenes de un usuario por ID de usuario
     * @param userId ID del usuario
     * @return Lista de órdenes del usuario
     */
    @Query("SELECT o FROM Orders o WHERE o.user.id = :userId ORDER BY o.date DESC")
    List<Orders> findByUserId(@Param("userId") Integer userId);

    /**
     * Busca todas las órdenes de un usuario cargando detalles
     * @param userId ID del usuario
     * @return Lista de órdenes del usuario con detalles
     */
    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.date DESC")
    List<Orders> findByUserIdWithDetails(@Param("userId") Integer userId);

    /**
     * Busca órdenes de un usuario específico con un estado particular
     * @param user Usuario dueño de las órdenes
     * @param status Estado de la orden a filtrar
     * @return Lista de órdenes del usuario con el estado especificado
     */
    List<Orders> findByUserAndStatus(Users user, OrderStatus status);

    /**
     * Busca órdenes de un usuario por ID con un estado particular
     * @param userId ID del usuario
     * @param status Estado de la orden
     * @return Lista de órdenes del usuario con el estado especificado
     */
    @Query("SELECT o FROM Orders o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.date DESC")
    List<Orders> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") OrderStatus status);

    // CONSULTAS POR ESTADO

    /**
     * Busca todas las órdenes con un estado específico
     * @param status Estado de las órdenes a buscar
     * @return Lista de órdenes con el estado especificado
     */
    List<Orders> findByStatus(OrderStatus status);

    /**
     * Busca todas las órdenes con un estado específico cargando detalles
     * @param status Estado de las órdenes
     * @return Lista de órdenes con estado y detalles
     */
    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "WHERE o.status = :status " +
            "ORDER BY o.date DESC")
    List<Orders> findByStatusWithDetails(@Param("status") OrderStatus status);

    /**
     * Cuenta el número de órdenes con un estado específico
     * @param status Estado de las órdenes
     * @return Número de órdenes con ese estado
     */
    long countByStatus(OrderStatus status);

    // CONSULTAS POR FECHA

    /**
     * Busca órdenes dentro de un rango de fechas
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de órdenes en el rango de fechas
     */
    @Query("SELECT o FROM Orders o WHERE o.date BETWEEN :startDate AND :endDate ORDER BY o.date DESC")
    List<Orders> findByDateBetween(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Busca órdenes recientes (últimos N días)
     * @return Lista de órdenes recientes
     */
    @Query("SELECT o FROM Orders o WHERE o.date >= :sinceDate ORDER BY o.date DESC")
    List<Orders> findRecentOrders(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Busca las últimas N órdenes
     * @param limit Número máximo de órdenes a devolver
     * @return Lista de las últimas órdenes
     */
    @Query("SELECT o FROM Orders o ORDER BY o.date DESC LIMIT :limit")
    List<Orders> findLatestOrders(@Param("limit") int limit);

    // CONSULTAS POR MÉTODO DE PAGO

    /**
     * Busca órdenes por método de pago
     * @param paymentMethod Método de pago
     * @return Lista de órdenes con el método de pago especificado
     */
    List<Orders> findByPaymentMethod(String paymentMethod);

    // CONSULTAS DE ESTADÍSTICAS

    /**
     * Calcula el total de ventas (suma de finalTotal)
     * @return Suma total de todas las órdenes
     */
    @Query("SELECT SUM(o.finalTotal) FROM Orders o WHERE o.status = 'COMPLETED'")
    BigDecimal getTotalSales();

    /**
     * Calcula el total de ventas por usuario
     * @param userId ID del usuario
     * @return Suma total de las órdenes del usuario
     */
    @Query("SELECT SUM(o.finalTotal) FROM Orders o WHERE o.user.id = :userId AND o.status = 'COMPLETED'")
    BigDecimal getTotalSalesByUser(@Param("userId") Integer userId);

    /**
     * Obtiene estadísticas de órdenes por estado
     * @return Lista de conteos por estado
     */
    @Query("SELECT o.status, COUNT(o) FROM Orders o GROUP BY o.status")
    List<Object[]> getOrderStatsByStatus();

    // CONSULTAS PARA FACTURACIÓN

    /**
     * Verifica si existe una orden para un usuario específico con un ID dado
     * @param orderId ID de la orden
     * @param userId ID del usuario
     * @return true si la orden existe y pertenece al usuario
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM Orders o WHERE o.id = :orderId AND o.user.id = :userId")
    boolean existsByIdAndUserId(@Param("orderId") Integer orderId, @Param("userId") Integer userId);

    /**
     * Busca órdenes que no tienen factura asociada
     * @return Lista de órdenes sin factura
     */
    @Query("SELECT o FROM Orders o WHERE o.id NOT IN (SELECT i.order.id FROM Invoice i) AND o.status = 'COMPLETED'")
    List<Orders> findOrdersWithoutInvoice();

    /**
     * Busca órdenes completadas dentro de un período para reportes
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de órdenes completadas en el período
     */
    @Query("SELECT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.status = 'COMPLETED' AND o.date BETWEEN :startDate AND :endDate " +
            "ORDER BY o.date DESC")
    List<Orders> findCompletedOrdersForReport(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}