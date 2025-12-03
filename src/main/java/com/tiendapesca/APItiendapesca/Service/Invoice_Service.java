package com.tiendapesca.APItiendapesca.Service;

import com.tiendapesca.APItiendapesca.Dtos.InvoicePdfDTO;
import com.tiendapesca.APItiendapesca.Dtos.InvoiceResponseDTO;
import com.tiendapesca.APItiendapesca.Dtos.OrderDetailDTO;
import com.tiendapesca.APItiendapesca.Dtos.ProductItemDTO;
import com.tiendapesca.APItiendapesca.Entities.Invoice;
import com.tiendapesca.APItiendapesca.Entities.Orders;
import com.tiendapesca.APItiendapesca.Repository.Invoice_Repository;
import com.tiendapesca.APItiendapesca.Repository.Orders_Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar operaciones relacionadas con facturas
 * Proporciona funcionalidades para generar, cancelar, enviar y consultar facturas
 */
@Service
@Transactional
public class Invoice_Service {

    private static final Logger logger = LoggerFactory.getLogger(Invoice_Service.class);

    private final Invoice_Repository invoiceRepository;
    private final Orders_Repository orderRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final Email_Service emailService;

    @Autowired
    public Invoice_Service(Invoice_Repository invoiceRepository,
                           Orders_Repository orderRepository,
                           PdfGeneratorService pdfGeneratorService,
                           Email_Service emailService) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.emailService = emailService;
    }

    /**
     * Genera un número único de factura
     */
    public String generateInvoiceNumber() {
        return "INV-" + LocalDateTime.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Genera y guarda una factura para una orden específica
     * CON MEJOR MANEJO DE ERRORES PARA PDF
     */
    @Transactional
    public Invoice generateAndSaveInvoice(Integer orderId) throws Exception {
        logger.info("🚀 INICIANDO generación de factura para orden ID: {}", orderId);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("❌ Orden no encontrada con ID: {}", orderId);
                    return new RuntimeException("Orden no encontrada con ID: " + orderId);
                });

        if (invoiceRepository.existsByOrder(order)) {
            logger.warn("⚠️ Ya existe una factura para la orden ID: {}", orderId);
            throw new RuntimeException("Ya existe una factura para esta orden");
        }

        // --- Lógica de Reintento para Número de Factura Único ---
        int maxRetries = 3;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            
            String invoiceNumber = generateInvoiceNumber();
            logger.debug("🔄 Intento {}: Número de factura generado: {}", attempt + 1, invoiceNumber);

            Invoice invoice = new Invoice();
            invoice.setOrder(order);
            invoice.setDate(LocalDateTime.now());
            invoice.setInvoiceNumber(invoiceNumber);
            
            try {
                // 1. PRIMERO guardar la factura en la base de datos
                logger.debug("💾 Guardando factura en BD...");
                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("✅ Factura guardada en BD con ID: {}", savedInvoice.getId());
                
                // 2. GENERAR PDF usando el DTO seguro
                logger.debug("📄 Iniciando generación de PDF...");
                String pdfPath = generateInvoicePdfSafely(savedInvoice);
                
                // 3. ACTUALIZAR factura con la URL del PDF
                savedInvoice.setPdfUrl(pdfPath);
                Invoice finalInvoice = invoiceRepository.save(savedInvoice);
                
                logger.info("🎉 FACTURA COMPLETADA - ID: {}, PDF: {}", finalInvoice.getId(), finalInvoice.getPdfUrl());
                return finalInvoice;
                
            } catch (DataIntegrityViolationException e) {
                logger.warn("⚠️ Colisión de número de factura: {}. Reintentando...", invoiceNumber);
                
                if (attempt == maxRetries - 1) {
                    logger.error("❌ Fallo definitivo después de {} intentos.", maxRetries, e);
                    throw new Exception("Fallo al generar número de factura único.", e);
                }
                
            } catch (Exception e) {
                logger.error("❌ Error crítico al generar factura para orden ID: {}", orderId, e);
                throw new Exception("Error al generar la factura: " + e.getMessage(), e);
            }
        }
        
        throw new Exception("Fallo inesperado después de reintentos.");
    }

    /**
     * MÉTODO NUEVO: Genera el PDF de forma segura con manejo de errores
     */
    private String generateInvoicePdfSafely(Invoice invoice) throws Exception {
        try {
            logger.debug("🔄 Convirtiendo Invoice a DTO...");
            InvoicePdfDTO invoicePdfDTO = convertToInvoicePdfDTO(invoice);
            
            logger.debug("🖨️ Generando bytes del PDF...");
            byte[] pdfBytes = pdfGeneratorService.generateInvoicePdf(invoicePdfDTO);
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new Exception("El PDF generado está vacío o es nulo");
            }
            
            logger.debug("✅ PDF generado - Tamaño: {} bytes", pdfBytes.length);
            
            logger.debug("💾 Guardando PDF en almacenamiento...");
            String pdfPath = pdfGeneratorService.savePdfToStorage(pdfBytes, invoice.getInvoiceNumber());
            
            if (pdfPath == null || pdfPath.trim().isEmpty()) {
                throw new Exception("La ruta del PDF es nula o vacía");
            }
            
            logger.debug("✅ PDF guardado en: {}", pdfPath);
            return pdfPath;
            
        } catch (Exception e) {
            logger.error("❌ ERROR en generateInvoicePdfSafely: {}", e.getMessage(), e);
            throw new Exception("Fallo al generar el PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte una entidad Invoice a InvoicePdfDTO para generar PDF sin recursión
     */
    private InvoicePdfDTO convertToInvoicePdfDTO(Invoice invoice) {
        logger.debug("🔄 Convirtiendo Invoice a DTO - Invoice ID: {}", invoice.getId());
        
        try {
            // Validaciones básicas
            if (invoice == null) throw new IllegalArgumentException("Invoice nulo");
            if (invoice.getOrder() == null) throw new IllegalArgumentException("Order nulo en invoice");
            
            Orders order = invoice.getOrder();
            
            // Datos del cliente con validación
            String customerName = "Cliente";
            String customerEmail = "No especificado";
            if (order.getUser() != null) {
                customerName = order.getUser().getName() != null ? order.getUser().getName() : "Cliente";
                customerEmail = order.getUser().getEmail() != null ? order.getUser().getEmail() : "No especificado";
            }
            
            // Convertir detalles del pedido
            List<ProductItemDTO> productItems = order.getOrderDetails().stream()
                .map(detail -> {
                    if (detail == null || detail.getProduct() == null) {
                        logger.warn("⚠️ OrderDetail o Product nulo encontrado");
                        return new ProductItemDTO(
                            "Producto no disponible",
                            java.math.BigDecimal.ZERO,
                            0,
                            java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO
                        );
                    }
                    return new ProductItemDTO(
                        detail.getProduct().getName(),
                        detail.getUnitPrice(),
                        detail.getQuantity(),
                        detail.getSubtotal(),
                        detail.getTax()
                    );
                })
                .collect(Collectors.toList());

            // Construir DTO
            InvoicePdfDTO dto = new InvoicePdfDTO(
                invoice.getInvoiceNumber(),
                invoice.getDate(),
                order.getPaymentMethod().toString(),
                customerName,
                customerEmail,
                order.getShippingAddress(),
                order.getPhone(),
                productItems,
                order.getTotalWithoutTax(),
                order.getTax(),
                order.getFinalTotal()
            );
            
            logger.debug("✅ DTO creado exitosamente");
            return dto;
            
        } catch (Exception e) {
            logger.error("❌ Error en convertToInvoicePdfDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al convertir invoice a DTO", e);
        }
    }

    /**
     * MÉTODO DE DIAGNÓSTICO: Verifica el estado del PDF
     */
    public String checkPdfStatus(Integer orderId) {
        try {
            Invoice invoice = getInvoiceForOrder(orderId);
            
            if (invoice.getPdfUrl() == null) {
                return "❌ PDF NO GENERADO - URL es nula";
            }
            
            if (!Files.exists(Paths.get(invoice.getPdfUrl()))) {
                return "❌ PDF NO ENCONTRADO - Archivo no existe en: " + invoice.getPdfUrl();
            }
            
            long fileSize = Files.size(Paths.get(invoice.getPdfUrl()));
            return String.format("✅ PDF OK - Tamaño: %d bytes, Ruta: %s", fileSize, invoice.getPdfUrl());
            
        } catch (Exception e) {
            return "❌ ERROR en diagnóstico: " + e.getMessage();
        }
    }

    /**
     * MÉTODO NUEVO: Reintenta generar el PDF para una factura existente
     */
    @Transactional
    public Invoice retryPdfGeneration(Integer orderId) throws Exception {
        logger.info("🔄 Reintentando generación de PDF para orden ID: {}", orderId);
        
        Invoice invoice = getInvoiceForOrder(orderId);
        
        try {
            String newPdfPath = generateInvoicePdfSafely(invoice);
            
            // Eliminar archivo anterior si existe
            if (invoice.getPdfUrl() != null && Files.exists(Paths.get(invoice.getPdfUrl()))) {
                Files.delete(Paths.get(invoice.getPdfUrl()));
            }
            
            invoice.setPdfUrl(newPdfPath);
            Invoice updatedInvoice = invoiceRepository.save(invoice);
            
            logger.info("✅ PDF regenerado exitosamente para factura: {}", invoice.getInvoiceNumber());
            return updatedInvoice;
            
        } catch (Exception e) {
            logger.error("❌ Error al regenerar PDF: {}", e.getMessage(), e);
            throw new Exception("Error al regenerar PDF: " + e.getMessage(), e);
        }
    }

    // Los demás métodos se mantienen igual...
    /**
     * Cancela una factura existente
     */
    @Transactional
    public void cancelInvoice(Integer orderId) {
        try {
            logger.info("Cancelando factura para orden ID: {}", orderId);

            Invoice invoice = invoiceRepository.findByOrderId(orderId).orElse(null);

            if (invoice != null) {
                invoice.setIsCanceled(true);
                invoice.setCancelationDate(LocalDateTime.now());
                invoiceRepository.save(invoice);
                logger.info("Factura cancelada exitosamente para orden ID: {}", orderId);
            } else {
                logger.warn("No se encontró factura para cancelar con orden ID: {}", orderId);
            }
        } catch (Exception e) {
            logger.error("Error al cancelar factura para orden ID: {}", orderId, e);
            throw new RuntimeException("Error al cancelar la factura", e);
        }
    }

    /**
     * Envía una factura por correo electrónico
     */
    @Transactional
    public void sendInvoiceByEmail(Integer orderId, String emailAddress) throws Exception {
        logger.info("Enviando factura por email para orden ID: {} a {}", orderId, emailAddress);

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    logger.error("Factura no encontrada para orden ID: {}", orderId);
                    return new RuntimeException("Factura no encontrada");
                });

        try {
            byte[] pdfBytes = Files.readAllBytes(Paths.get(invoice.getPdfUrl()));

            emailService.sendEmailWithAttachment(
                    emailAddress,
                    "Factura #" + invoice.getInvoiceNumber(),
                    "Adjunto encontrará la factura de su compra reciente.",
                    "factura_" + invoice.getInvoiceNumber() + ".pdf",
                    pdfBytes
            );

            logger.info("Factura enviada exitosamente a {}", emailAddress);
        } catch (Exception e) {
            logger.error("Error al enviar factura por email para orden ID: {}", orderId, e);
            throw new Exception("Error al enviar la factura por email", e);
        }
    }

    /**
     * Obtiene la factura asociada a una orden
     */
    public Invoice getInvoiceForOrder(Integer orderId) {
        logger.debug("Buscando factura para orden ID: {}", orderId);

        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    logger.error("Factura no encontrada para orden ID: {}", orderId);
                    return new RuntimeException("Factura no encontrada para la orden: " + orderId);
                });
    }

    /**
     * Obtiene el archivo PDF de una factura
     */
    public byte[] getInvoicePdf(Integer orderId) throws IOException {
        logger.debug("Obteniendo PDF de factura para orden ID: {}", orderId);

        Invoice invoice = getInvoiceForOrder(orderId);
        return Files.readAllBytes(Paths.get(invoice.getPdfUrl()));
    }

    /**
     * Convierte una entidad Invoice a un DTO de respuesta
     */
    public InvoiceResponseDTO convertInvoiceToResponseDTO(Invoice invoice) {
        Orders order = invoice.getOrder();

        List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream().map(item -> {
            OrderDetailDTO dto = new OrderDetailDTO();
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setSubtotal(item.getSubtotal());
            dto.setTax(item.getTax());
            dto.setTotal(item.getTotal());
            return dto;
        }).collect(Collectors.toList());

        return new InvoiceResponseDTO(
            order.getUser().getEmail(),
            order.getFinalTotal(),
            order.getId(),
            invoice.getInvoiceNumber(),
            invoice.getDate(),
            detailDTOs
        );
    }

    /**
     * Método alternativo para obtener el PDF usando DTO seguro
     */
    public byte[] generateInvoicePdfSafe(Integer orderId) throws Exception {
        logger.debug("Generando PDF seguro para orden ID: {}", orderId);

        Invoice invoice = getInvoiceForOrder(orderId);
        InvoicePdfDTO invoicePdfDTO = convertToInvoicePdfDTO(invoice);
        return pdfGeneratorService.generateInvoicePdf(invoicePdfDTO);
    }
}