package com.tiendapesca.APItiendapesca.Controller;

import com.tiendapesca.APItiendapesca.Dtos.OrderRequestDTO;
import com.tiendapesca.APItiendapesca.Dtos.OrderResponseDTO;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Service.Orders_Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar operaciones relacionadas con órdenes de compra
 * Proporciona endpoints para crear, consultar y cancelar órdenes
 */
@RestController
@RequestMapping("/api/orders")
public class Orders_Controller {

    private final Orders_Service ordersService;

    /**
     * Constructor para inyección de dependencias del servicio de ordenes
     * @param ordersService Servicio para operaciones con órdenes
     */
    @Autowired
    public Orders_Controller(Orders_Service ordersService) {
        this.ordersService = ordersService;
    }
    
    /**
     * Crea una nueva orden a partir del carrito de compras del usuario
     * @param user Usuario autenticado
     * @param orderRequest DTO con los datos de la orden
     * @return ResponseEntity con la orden creada y estado HTTP 201
     */
    @PostMapping("/add")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody OrderRequestDTO orderRequest) {
        OrderResponseDTO orderResponse = ordersService.createOrderFromCart(user, orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }
    
    /**
     * Obtiene todas las órdenes del usuario autenticado
     * @param user Usuario autenticado
     * @return Lista de DTOs con las órdenes del usuario (sin datos sensibles)
     */
    @GetMapping("/get")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(
            @AuthenticationPrincipal Users user) {
        List<OrderResponseDTO> orders = ordersService.getUserOrders(user.getId());
        return ResponseEntity.ok(orders);
    }

    /**
     * Obtiene los detalles de una orden específica
     * @param user Usuario autenticado
     * @param orderId ID de la orden a consultar
     * @return DTO con los detalles de la orden (sin datos sensibles del usuario)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderDetails(
            @AuthenticationPrincipal Users user,
            @PathVariable Integer orderId) {
        OrderResponseDTO order = ordersService.getOrderDetails(orderId, user);
        return ResponseEntity.ok(order);
    }
     
    /**
     * Cancela una orden existente
     * @param user Usuario autenticado
     * @param orderId ID de la orden a cancelar
     * @return ResponseEntity con estado HTTP 204 (No Content)
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal Users user,
            @PathVariable Integer orderId) {
        ordersService.cancelOrder(orderId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene los detalles completos de una orden incluyendo sus items
     * @param user Usuario autenticado
     * @param orderId ID de la orden a consultar
     * @return DTO con los detalles completos de la orden y sus items (sin datos sensibles)
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<OrderResponseDTO> getOrderWithItems(
            @AuthenticationPrincipal Users user,
            @PathVariable Integer orderId) {
        OrderResponseDTO order = ordersService.getOrderDetails(orderId, user);
        return ResponseEntity.ok(order);
    }

    /**
     * Endpoint para administradores - Obtiene todas las órdenes del sistema
     * @return Lista de todas las órdenes (sin datos sensibles de usuarios)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<OrderResponseDTO> orders = ordersService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Endpoint para administradores - Actualiza el estado de una orden
     * @param orderId ID de la orden a actualizar
     * @param status Nuevo estado de la orden
     * @return ResponseEntity con estado HTTP 200
     */
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam String status) {
        // Convertir String a OrderStatus enum
        try {
            com.tiendapesca.APItiendapesca.Entities.OrderStatus orderStatus = 
                com.tiendapesca.APItiendapesca.Entities.OrderStatus.valueOf(status.toUpperCase());
            ordersService.updateOrderStatus(orderId, orderStatus);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}