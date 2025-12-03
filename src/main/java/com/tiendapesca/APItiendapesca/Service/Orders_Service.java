package com.tiendapesca.APItiendapesca.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tiendapesca.APItiendapesca.Dtos.CartItemRespoDTO;
import com.tiendapesca.APItiendapesca.Dtos.OrderDetailDTO;
import com.tiendapesca.APItiendapesca.Dtos.OrderRequestDTO;
import com.tiendapesca.APItiendapesca.Dtos.OrderResponseDTO;
import com.tiendapesca.APItiendapesca.Dtos.UserDTO;
import com.tiendapesca.APItiendapesca.Entities.Invoice;
import com.tiendapesca.APItiendapesca.Entities.OrderDetail;
import com.tiendapesca.APItiendapesca.Entities.OrderStatus;
import com.tiendapesca.APItiendapesca.Entities.Orders;
import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Repository.OrderDetail_Repository;
import com.tiendapesca.APItiendapesca.Repository.Orders_Repository;
import com.tiendapesca.APItiendapesca.Repository.Product_Repository;
import com.tiendapesca.APItiendapesca.Repository.Users_Repository;

/**
 * Servicio para gestionar operaciones relacionadas con órdenes de compra
 * Proporciona funcionalidades para crear, consultar y cancelar órdenes
 */
@Service
public class Orders_Service {

    private final Orders_Repository orderRepository;
    private final OrderDetail_Repository orderDetailRepository;
    private final Cart_Service cartService;
    private final Product_Repository productRepository;
    private final Users_Repository userRepository;
    private final Invoice_Service invoiceService;

    /**
     * Constructor para inyección de dependencias
     * @param orderRepository Repositorio de órdenes
     * @param orderDetailRepository Repositorio de detalles de órdenes
     * @param cartService Servicio del carrito
     * @param productRepository Repositorio de productos
     * @param userRepository Repositorio de usuarios
     * @param invoiceService Servicio de facturas
     */
    @Autowired
    public Orders_Service(Orders_Repository orderRepository,
                       OrderDetail_Repository orderDetailRepository,
                       Cart_Service cartService,
                       Product_Repository productRepository,
                       Users_Repository userRepository,
                       Invoice_Service invoiceService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.invoiceService = invoiceService;
    }

    /**
     * Crea una nueva orden a partir del carrito de compras del usuario
     * @param user Usuario autenticado
     * @param orderRequest DTO con información de la orden
     * @return DTO con la respuesta de la orden creada
     */
    @Transactional
    public OrderResponseDTO createOrderFromCart(Users user, OrderRequestDTO orderRequest) {
        // Validar usuario
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autenticado");
        }

        // Obtener items del carrito
        List<CartItemRespoDTO> cartItems = cartService.getCartItems(user.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
        }

        // Calcular totales
        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Suponiendo un impuesto del 13% (como en tu esquema SQL)
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        // Crear la orden
        Orders order = new Orders();
        order.setUser(user);
        order.setDate(LocalDateTime.now());
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setPhone(orderRequest.getPhone());
        order.setTotalWithoutTax(subtotal);
        order.setTax(tax);
        order.setFinalTotal(total);
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setStatus(OrderStatus.PROCESSING);

        // Guardar la orden
        Orders savedOrder = orderRepository.save(order);

        // Crear los detalles de la orden
        for (CartItemRespoDTO cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Producto no encontrado: " + cartItem.getProductId()));

            // Verificar stock
            if (product.getStock() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el producto: " + product.getName());
            }

            // Calcular totales para el detalle
            BigDecimal itemSubtotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal itemTax = itemSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = itemSubtotal.add(itemTax).setScale(2, RoundingMode.HALF_UP);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(cartItem.getQuantity());
            orderDetail.setUnitPrice(cartItem.getUnitPrice());
            orderDetail.setSubtotal(itemSubtotal);
            orderDetail.setTax(itemTax);
            orderDetail.setTotal(itemTotal);

            orderDetailRepository.save(orderDetail);

            // Actualizar stock del producto
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Generar factura automáticamente
        try {
            Invoice invoice = invoiceService.generateAndSaveInvoice(savedOrder.getId());
            
            // Opcional: enviar factura por email automáticamente
            invoiceService.sendInvoiceByEmail(savedOrder.getId(), user.getEmail());
        } catch (Exception e) {
            // Loggear el error pero no fallar la orden
            System.err.println("Error generando factura: " + e.getMessage());
        }

        // Vaciar el carrito
        cartService.clearCart(user);

        // Convertir a DTO para la respuesta
        return convertToOrderResponseDTO(savedOrder);
    }

    /**
     * Obtiene todas las órdenes de un usuario
     * @param userId ID del usuario
     * @return Lista de DTOs con las órdenes del usuario
     */
    public List<OrderResponseDTO> getUserOrders(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Orders> orders = orderRepository.findByUser(user);
        return orders.stream()
                .map(this::convertToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los detalles de una orden específica
     * @param orderId ID de la orden
     * @param user Usuario autenticado
     * @return DTO con los detalles de la orden
     */
    public OrderResponseDTO getOrderDetails(Integer orderId, Users user) {
        Orders order = getOrderEntity(orderId);

        // Verificar que el usuario sea el dueño de la orden
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta orden");
        }

        return convertToOrderResponseDTO(order);
    }

    /**
     * Obtiene la entidad Orders por ID
     * @param orderId ID de la orden
     * @return Entidad Orders
     */
    public Orders getOrderEntity(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + orderId));
    }

    /**
     * Convierte una entidad Orders a un DTO de respuesta
     * @param order Orden a convertir
     * @return DTO con la información de la orden
     */
    private OrderResponseDTO convertToOrderResponseDTO(Orders order) {
        OrderResponseDTO responseDTO = new OrderResponseDTO();
        
        responseDTO.setOrderId(order.getId());  // Ahora ambos son Integer
        responseDTO.setOrderDate(order.getDate());
        responseDTO.setShippingAddress(order.getShippingAddress());
        responseDTO.setPhone(order.getPhone());
        responseDTO.setSubtotal(order.getTotalWithoutTax());
        responseDTO.setTax(order.getTax());
        responseDTO.setTotal(order.getFinalTotal());
        responseDTO.setPaymentMethod(order.getPaymentMethod());
        responseDTO.setStatus(order.getStatus());

        // Convertir usuario a UserDTO (sin datos sensibles)
        UserDTO userDTO = convertToUserDTO(order.getUser());
        responseDTO.setUser(userDTO);

        // Convertir detalles de la orden
        if (order.getOrderDetails() != null) {
            List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream()
                    .map(this::convertToOrderDetailDTO)
                    .collect(Collectors.toList());
            responseDTO.setOrderDetails(detailDTOs);
        }

        return responseDTO;
    }

    /**
     * Convierte una entidad Users a un UserDTO seguro (sin datos sensibles)
     * @param user Usuario a convertir
     * @return UserDTO con información segura
     */
    private UserDTO convertToUserDTO(Users user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setRegistrationDate(user.getRegistrationDate());
        // NO incluir: password, authorities, username, accountNonLocked, role, etc.
        return userDTO;
    }
    
    
    
    /**
     * Convierte una entidad OrderDetail a un DTO
     * @param orderDetail Detalle de orden a convertir
     * @return DTO con la información del detalle
     */
    private OrderDetailDTO convertToOrderDetailDTO(OrderDetail orderDetail) {
        OrderDetailDTO detailDTO = new OrderDetailDTO();
        detailDTO.setProductId(orderDetail.getProduct().getId());
        detailDTO.setProductName(orderDetail.getProduct().getName());
        detailDTO.setQuantity(orderDetail.getQuantity());
        detailDTO.setUnitPrice(orderDetail.getUnitPrice());
        detailDTO.setSubtotal(orderDetail.getSubtotal());
        detailDTO.setTax(orderDetail.getTax());
        detailDTO.setTotal(orderDetail.getTotal());
        return detailDTO;
    }

    /**
     * Cancela una orden existente
     * @param orderId ID de la orden a cancelar
     * @param user Usuario Autenticado
     */
    @Transactional
    public void cancelOrder(Integer orderId, Users user) {
        Orders order = getOrderEntity(orderId);

        // Verificar que el usuario sea el dueño de la orden
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para cancelar esta orden");
        }

        // Solo se pueden cancelar órdenes en procesamiento
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Solo se pueden cancelar órdenes en estado PROCESSING");
        }

        // Devolver el stock a los productos
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);
        }

        // Actualizar estado de la orden
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Opcional: Cancelar factura asociada si existe
        try {
            invoiceService.cancelInvoice(orderId);
        } catch (Exception e) {
            System.err.println("Error cancelando factura: " + e.getMessage());
        }
    }

    /**
     * Obtiene todas las órdenes (para administradores)
     * @return Lista de todas las órdenes
     */
    public List<OrderResponseDTO> getAllOrders() {
        List<Orders> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::convertToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el estado de una orden (para administradores)
     * @param orderId ID de la orden
     * @param status Nuevo estado
     */
    @Transactional
    public void updateOrderStatus(Integer orderId, OrderStatus status) {
        Orders order = getOrderEntity(orderId);
        order.setStatus(status);
        orderRepository.save(order);
    }
}