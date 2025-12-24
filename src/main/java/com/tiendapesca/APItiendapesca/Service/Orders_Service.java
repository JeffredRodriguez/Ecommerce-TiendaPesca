package com.tiendapesca.APItiendapesca.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Transactional
public class Orders_Service {

    private static final Logger logger = LoggerFactory.getLogger(Orders_Service.class);

    private final Orders_Repository orderRepository;
    private final OrderDetail_Repository orderDetailRepository;
    private final Cart_Service cartService;
    private final Product_Repository productRepository;
    private final Users_Repository userRepository;
    private final Invoice_Service invoiceService;

    /**
     * Constructor para inyección de dependencias
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
        logger.info("Creando orden para usuario: {}", user.getEmail());

        // Validar usuario
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autenticado");
        }

        // Obtener items del carrito
        List<CartItemRespoDTO> cartItems = cartService.getCartItems(user.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
        }

        logger.info("{} items en el carrito", cartItems.size());

        // Calcular totales
        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        logger.info("Totales calculados - Subtotal: {}, Tax: {}, Total: {}", subtotal, tax, total);

        // Crear la entidad Orders
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

        // Crear lista de OrderDetails antes de guardar
        List<OrderDetail> orderDetails = new ArrayList<>();

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
            BigDecimal itemSubtotal = cartItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal itemTax = itemSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = itemSubtotal.add(itemTax).setScale(2, RoundingMode.HALF_UP);

            // crea OrderDetail con la relación bidireccional
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);  //
            orderDetail.setProduct(product);
            orderDetail.setQuantity(cartItem.getQuantity());
            orderDetail.setUnitPrice(cartItem.getUnitPrice());
            orderDetail.setSubtotal(itemSubtotal);
            orderDetail.setTax(itemTax);
            orderDetail.setTotal(itemTotal);

            orderDetails.add(orderDetail);

            // Actualizar stock del producto
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            logger.debug("Detalle creado - Producto: {}, Cantidad: {}, Subtotal: {}",
                    product.getName(), cartItem.getQuantity(), itemSubtotal);
        }

        // Establecer orderDetails en la orden
        order.setOrderDetails(orderDetails);
        logger.info("{} detalles asignados a la orden", orderDetails.size());

        // Guardar la orden (CASCADE guardará los detalles)
        logger.info("Guardando orden en base de datos...");
        Orders savedOrder = orderRepository.save(order);
        logger.info("Orden guardada con ID: {}", savedOrder.getId());

        // Verifica que los detalles se guardaron
        if (savedOrder.getOrderDetails() != null) {
            logger.info("OrderDetails guardados: {}", savedOrder.getOrderDetails().size());
        } else {
            logger.error("ERROR: OrderDetails es NULL después de guardar!");
        }

        // Generar factura automáticamente (SIN ENVÍO DE EMAIL)
        try {
            logger.info("Generando factura para orden ID: {}", savedOrder.getId());
            Invoice invoice = invoiceService.generateAndSaveInvoice(savedOrder.getId());
            logger.info("Factura PDF generada exitosamente. Número: {}, Archivo: {}",
                    invoice.getInvoiceNumber(), invoice.getPdfUrl());

            // EMAIL DESHABILITADO TEMPORALMENTE
            logger.info("Factura PDF creada exitosamente. El envío por email está deshabilitado temporalmente.");

        } catch (Exception invoiceError) {
            logger.error("ERROR al generar factura: {}", invoiceError.getMessage());
            // NO fallar la orden si la factura falla
            logger.warn("Continuando sin factura debido a error. La orden se creó exitosamente.");
        }

        // Vaciar el carrito
        cartService.clearCart(user);
        logger.info("Carrito limpiado");

        // FORZAR CARGA COMPLETA ANTES DE CONVERTIR A DTO
        Orders completeOrder = orderRepository.findByIdWithAllDetails(savedOrder.getId())
                .orElse(savedOrder);

        logger.info("Orden completada exitosamente. ID: {}, Estado: {}",
                completeOrder.getId(), completeOrder.getStatus());

        // Convertir a DTO para la respuesta
        OrderResponseDTO response = convertToOrderResponseDTO(completeOrder);

        //AÑADIR INFORMACIÓN DE LA FACTURA A LA RESPUESTA
        try {
            Invoice invoice = invoiceService.getInvoiceForOrder(completeOrder.getId());
            response.setInvoiceNumber(invoice.getInvoiceNumber());
            response.setInvoiceDate(invoice.getDate());
            response.setPdfUrl(invoice.getPdfUrl());
            logger.info("Información de factura añadida a la respuesta: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            logger.warn("No se pudo añadir información de factura a la respuesta");
        }

        return response;
    }

    /**
     * Obtiene todas las órdenes de un usuario
     * @param userId ID del usuario
     * @return Lista de DTOs con las órdenes del usuario
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));


        List<Orders> orders = orderRepository.findByUserIdWithDetails(userId);

        logger.info("Obteniendo {} órdenes para usuario ID: {}", orders.size(), userId);

        // Convertir a DTOs y añadir información de facturas
        return orders.stream()
                .map(order -> {
                    OrderResponseDTO dto = convertToOrderResponseDTO(order);
                    try {
                        Invoice invoice = invoiceService.getInvoiceForOrder(order.getId());
                        dto.setInvoiceNumber(invoice.getInvoiceNumber());
                        dto.setInvoiceDate(invoice.getDate());
                        dto.setPdfUrl(invoice.getPdfUrl());
                    } catch (Exception e) {
                        // Si no hay factura, no es un error grave
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los detalles de una orden específica
     * @param orderId ID de la orden
     * @param user Usuario autenticado
     * @return DTO con los detalles de la orden
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetails(Integer orderId, Users user) {

        Orders order = orderRepository.findByIdWithAllDetails(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + orderId));

        // Verificar que el usuario sea el dueño de la orden
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para ver esta orden");
        }

        logger.info("Obteniendo detalles de orden ID: {} para usuario: {}",
                orderId, user.getEmail());

        OrderResponseDTO response = convertToOrderResponseDTO(order);

        // Añadir información de la factura si existe
        try {
            Invoice invoice = invoiceService.getInvoiceForOrder(orderId);
            response.setInvoiceNumber(invoice.getInvoiceNumber());
            response.setInvoiceDate(invoice.getDate());
            response.setPdfUrl(invoice.getPdfUrl());
        } catch (Exception e) {
            logger.debug("La orden {} no tiene factura asociada", orderId);
        }

        return response;
    }

    /**
     * Obtiene la entidad Orders por ID (con todas las relaciones)
     * @param orderId ID de la orden
     * @return Entidad Orders con todas las relaciones cargadas
     */
    @Transactional(readOnly = true)
    public Orders getOrderEntity(Integer orderId) {
        return orderRepository.findByIdWithAllDetails(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + orderId));
    }

    /**
     * Convierte una entidad Orders a un DTO de respuesta
     */
    private OrderResponseDTO convertToOrderResponseDTO(Orders order) {
        logger.debug("Convirtiendo orden ID: {} a DTO", order.getId());

        OrderResponseDTO responseDTO = new OrderResponseDTO();

        responseDTO.setOrderId(order.getId());
        responseDTO.setOrderDate(order.getDate());
        responseDTO.setShippingAddress(order.getShippingAddress());
        responseDTO.setPhone(order.getPhone());
        responseDTO.setSubtotal(order.getTotalWithoutTax());
        responseDTO.setTax(order.getTax());
        responseDTO.setTotal(order.getFinalTotal());
        responseDTO.setPaymentMethod(order.getPaymentMethod());
        responseDTO.setStatus(order.getStatus());

        // Convertir usuario a UserDTO
        UserDTO userDTO = convertToUserDTO(order.getUser());
        responseDTO.setUser(userDTO);

        //MANEJO SEGURO DE OrderDetails
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream()
                    .map(this::convertToOrderDetailDTO)
                    .collect(Collectors.toList());
            responseDTO.setOrderDetails(detailDTOs);
            logger.debug("{} detalles convertidos a DTO", detailDTOs.size());
        } else {
            logger.warn("Orden ID: {} no tiene detalles o son null", order.getId());
            responseDTO.setOrderDetails(new ArrayList<>());
        }

        return responseDTO;
    }

    /**
     * Convierte una entidad Users a un UserDTO seguro
     */
    private UserDTO convertToUserDTO(Users user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setRegistrationDate(user.getRegistrationDate());
        return userDTO;
    }

    /**
     * Convierte una entidad OrderDetail a un DTO
     */
    private OrderDetailDTO convertToOrderDetailDTO(OrderDetail orderDetail) {
        OrderDetailDTO detailDTO = new OrderDetailDTO();

        if (orderDetail.getProduct() != null) {
            detailDTO.setProductId(orderDetail.getProduct().getId());
            detailDTO.setProductName(orderDetail.getProduct().getName());
        } else {
            detailDTO.setProductId(0);
            detailDTO.setProductName("Producto no disponible");
            logger.warn("OrderDetail sin producto asociado");
        }

        detailDTO.setQuantity(orderDetail.getQuantity());
        detailDTO.setUnitPrice(orderDetail.getUnitPrice());
        detailDTO.setSubtotal(orderDetail.getSubtotal());
        detailDTO.setTax(orderDetail.getTax());
        detailDTO.setTotal(orderDetail.getTotal());

        return detailDTO;
    }

    /**
     * Cancela una orden existente
     */
    @Transactional
    public void cancelOrder(Integer orderId, Users user) {
        logger.info("Cancelando orden ID: {} para usuario: {}", orderId, user.getEmail());

        Orders order = getOrderEntity(orderId);

        // Verificar permisos
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para cancelar esta orden");
        }

        // Solo se pueden cancelar órdenes en procesamiento
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se pueden cancelar órdenes en estado PROCESSING. Estado actual: " + order.getStatus());
        }

        // Devolver el stock a los productos
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    product.setStock(product.getStock() + detail.getQuantity());
                    productRepository.save(product);
                    logger.debug("Stock devuelto para producto ID: {}, cantidad: {}",
                            product.getId(), detail.getQuantity());
                }
            }
        }

        // Actualizar estado de la orden
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logger.info("Orden ID: {} cancelada", orderId);

        // Cancelar factura asociada si existe
        try {
            invoiceService.cancelInvoice(orderId);
            logger.info("Factura cancelada para orden ID: {}", orderId);
        } catch (Exception e) {
            logger.warn("No se pudo cancelar la factura: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todas las órdenes (para administradores)
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        List<Orders> orders = orderRepository.findAll();
        logger.info("Obteniendo todas las órdenes. Total: {}", orders.size());

        // Para cada orden, cargar detalles si es necesario
        return orders.stream()
                .map(order -> {
                    // Si no tiene detalles cargados, recargar con detalles
                    if (order.getOrderDetails() == null) {
                        return orderRepository.findByIdWithDetails(order.getId())
                                .orElse(order);
                    }
                    return order;
                })
                .map(order -> {
                    OrderResponseDTO dto = convertToOrderResponseDTO(order);
                    // Añadir información de factura si existe
                    try {
                        Invoice invoice = invoiceService.getInvoiceForOrder(order.getId());
                        dto.setInvoiceNumber(invoice.getInvoiceNumber());
                        dto.setInvoiceDate(invoice.getDate());
                        dto.setPdfUrl(invoice.getPdfUrl());
                    } catch (Exception e) {
                        // No hay factura, continuar
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el estado de una orden (para administradores)
     */
    @Transactional
    public void updateOrderStatus(Integer orderId, OrderStatus status) {
        logger.info("Actualizando estado de orden ID: {} a {}", orderId, status);

        Orders order = getOrderEntity(orderId);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        orderRepository.save(order);

        logger.info("Estado de orden ID: {} actualizado de {} a {}",
                orderId, oldStatus, status);

        // Si se completa la orden, generar factura si no existe
        if (status == OrderStatus.COMPLETED && oldStatus != OrderStatus.COMPLETED) {
            try {
                // Verificar si ya existe factura
                invoiceService.getInvoiceForOrder(orderId);
                logger.info("La orden ya tiene factura asociada");
            } catch (Exception e) {
                // No existe factura, generarla
                try {
                    Invoice invoice = invoiceService.generateAndSaveInvoice(orderId);
                    logger.info("Factura generada automáticamente: {}", invoice.getInvoiceNumber());
                } catch (Exception invoiceError) {
                    logger.error("Error al generar factura automática: {}", invoiceError.getMessage());
                }
            }
        }
    }

    /**
     * Método para verificar el estado de una factura
     * @param orderId ID de la orden
     * @return Estado de la factura
     */
    public String checkInvoiceStatus(Integer orderId) {
        try {
            return invoiceService.checkPdfStatus(orderId);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}