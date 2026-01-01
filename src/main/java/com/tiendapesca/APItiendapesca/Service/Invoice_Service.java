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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la gestion integral de facturas, incluyendo generacion,
 * almacenamiento fisico de archivos PDF, persistencia en base de datos y envio por correo.
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
     * Genera un identificador unico para la factura basado en el año actual y un UUID corto.
     * * @return Cadena de texto con el formato INV-YYYY-XXXXXXXX.
     */
    public String generateInvoiceNumber() {
        return "INV-" + LocalDateTime.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Procesa la creacion de una factura vinculada a una orden, genera su archivo PDF
     * Gestiona posibles duplicados mediante reintentos.
     * * @param orderId Identificador de la orden a facturar.
     * @return Entidad Invoice persistida con la ruta del PDF.
     * @throws Exception Si la orden no existe, ya esta facturada o fallan los reintentos de guardado.
     */
    @Transactional
    public Invoice generateAndSaveInvoice(Integer orderId) throws Exception {
        logger.info("Iniciando generacion de factura para orden ID: {}", orderId);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Orden no encontrada con ID: {}", orderId);
                    return new RuntimeException("Orden no encontrada con ID: " + orderId);
                });

        if (invoiceRepository.existsByOrder(order)) {
            logger.warn("Ya existe una factura para la orden ID: {}", orderId);
            throw new RuntimeException("Ya existe una factura para esta orden");
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String invoiceNumber = generateInvoiceNumber();
            Invoice invoice = new Invoice();
            invoice.setOrder(order);
            invoice.setDate(LocalDateTime.now());
            invoice.setInvoiceNumber(invoiceNumber);

            try {
                logger.debug("Guardando factura inicial en BD");
                Invoice savedInvoice = invoiceRepository.save(invoice);

                logger.debug("Generando PDF");
                String pdfPath = generateInvoicePdfSafely(savedInvoice);

                savedInvoice.setPdfUrl(pdfPath);
                return invoiceRepository.save(savedInvoice);

            } catch (DataIntegrityViolationException e) {
                if (attempt == maxRetries - 1) throw new Exception("Fallo al generar numero de factura unico", e);
            } catch (Exception e) {
                logger.error("Error critico: {}", e.getMessage());
                throw new Exception("Error al generar la factura: " + e.getMessage(), e);
            }
        }
        throw new Exception("Fallo inesperado despues de reintentos.");
    }

    /**
     * Coordina la conversion de datos al formato DTO y la creacion fisica del archivo PDF.
     * * @param invoice Entidad de la factura.
     * @return Ruta absoluta o relativa donde se almaceno el archivo.
     * @throws Exception Si el contenido del PDF es nulo o falla el almacenamiento.
     */
    private String generateInvoicePdfSafely(Invoice invoice) throws Exception {
        try {
            InvoicePdfDTO invoicePdfDTO = convertToInvoicePdfDTO(invoice);
            byte[] pdfBytes = pdfGeneratorService.generateInvoicePdf(invoicePdfDTO);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new Exception("El PDF generado esta vacio");
            }

            return pdfGeneratorService.savePdfToStorage(pdfBytes, invoice.getInvoiceNumber());
        } catch (Exception e) {
            logger.error("Error en generateInvoicePdfSafely: {}", e.getMessage());
            throw new Exception("Fallo al generar el PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Transforma una entidad Invoice y su orden asociada en un objeto de transferencia de datos (DTO)
     * optimizado para la generacion del documento PDF.
     * * @param invoice Entidad factura.
     * @return Objeto InvoicePdfDTO con informacion detallada de cliente y productos.
     */
    private InvoicePdfDTO convertToInvoicePdfDTO(Invoice invoice) {
        logger.debug("Convirtiendo Invoice a DTO - ID: {}", invoice.getId());

        try {
            if (invoice == null || invoice.getOrder() == null) {
                throw new IllegalArgumentException("Invoice u Orden son nulos");
            }

            Orders order = invoice.getOrder();

            String customerName = "Cliente Generico";
            String customerEmail = "Sin correo";
            if (order.getUser() != null) {
                customerName = order.getUser().getName() != null ? order.getUser().getName() : customerName;
                customerEmail = order.getUser().getEmail() != null ? order.getUser().getEmail() : customerEmail;
            }

            String paymentMethod = "No especificado";
            if (order.getPaymentMethod() != null) {
                paymentMethod = order.getPaymentMethod().toString();
            }

            List<ProductItemDTO> productItems = Collections.emptyList();
            if (order.getOrderDetails() != null) {
                productItems = order.getOrderDetails().stream()
                        .map(detail -> {
                            String pName = (detail.getProduct() != null) ? detail.getProduct().getName() : "Producto Desconocido";
                            return new ProductItemDTO(
                                    pName,
                                    detail.getUnitPrice(),
                                    detail.getQuantity(),
                                    detail.getSubtotal(),
                                    detail.getTax()
                            );
                        })
                        .collect(Collectors.toList());
            } else {
                logger.warn("OrderDetails es nulo para la orden ID: {}. Se usara lista vacia.", order.getId());
            }

            return new InvoicePdfDTO(
                    invoice.getInvoiceNumber(),
                    invoice.getDate(),
                    paymentMethod,
                    customerName,
                    customerEmail,
                    order.getShippingAddress(),
                    order.getPhone(),
                    productItems,
                    order.getTotalWithoutTax(),
                    order.getTax(),
                    order.getFinalTotal()
            );

        } catch (Exception e) {
            logger.error("Error en convertToInvoicePdfDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al convertir invoice a DTO: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica la existencia del archivo fisico asociado a una factura en el sistema de archivos.
     * * @param orderId Identificador de la orden.
     * @return Estado descriptivo del archivo (OK, NO GENERADO, NO EXISTE).
     */
    public String checkPdfStatus(Integer orderId) {
        try {
            Invoice invoice = getInvoiceForOrder(orderId);
            if (invoice.getPdfUrl() == null) return "PDF NO GENERADO";
            if (!Files.exists(Paths.get(invoice.getPdfUrl()))) return "ARCHIVO NO EXISTE";
            return "PDF OK - " + Files.size(Paths.get(invoice.getPdfUrl())) + " bytes";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Regenera el archivo PDF de una factura existente, eliminando el archivo anterior si existe.
     * * @param orderId Identificador de la orden asociada.
     * @return Entidad Invoice actualizada con la nueva ruta.
     * @throws Exception Si ocurre un error durante la generacion o el guardado.
     */
    @Transactional
    public Invoice retryPdfGeneration(Integer orderId) throws Exception {
        Invoice invoice = getInvoiceForOrder(orderId);
        String newPdfPath = generateInvoicePdfSafely(invoice);

        if (invoice.getPdfUrl() != null) {
            try {
                Files.deleteIfExists(Paths.get(invoice.getPdfUrl()));
            } catch (IOException e) {
                logger.warn("No se pudo eliminar el archivo PDF anterior: {}", e.getMessage());
            }
        }

        invoice.setPdfUrl(newPdfPath);
        return invoiceRepository.save(invoice);
    }

    /**
     * Marca una factura como cancelada y registra la fecha de cancelacion.
     * * @param orderId Identificador de la orden facturada.
     */
    @Transactional
    public void cancelInvoice(Integer orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId).orElse(null);
        if (invoice != null) {
            invoice.setIsCanceled(true);
            invoice.setCancelationDate(LocalDateTime.now());
            invoiceRepository.save(invoice);
        }
    }

    /**
     * Recupera el archivo PDF y lo envia como adjunto por correo electronico.
     * * @param orderId Identificador de la orden.
     * @param emailAddress Direccion de correo destino.
     * @throws Exception Si el archivo no es legible o falla el servicio de correo.
     */
    @Transactional
    public void sendInvoiceByEmail(Integer orderId, String emailAddress) throws Exception {
        Invoice invoice = getInvoiceForOrder(orderId);
        byte[] pdfBytes = Files.readAllBytes(Paths.get(invoice.getPdfUrl()));
        emailService.sendEmailWithAttachment(
                emailAddress,
                "Factura #" + invoice.getInvoiceNumber(),
                "Adjunto encontrara su factura.",
                "factura_" + invoice.getInvoiceNumber() + ".pdf",
                pdfBytes
        );
    }

    /**
     * Busca la factura asociada a una orden especifica en la base de datos.
     * * @param orderId Identificador de la orden.
     * @return Entidad Invoice encontrada.
     * @throws RuntimeException Si no se encuentra ningun registro.
     */
    public Invoice getInvoiceForOrder(Integer orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada para orden: " + orderId));
    }

    /**
     * Obtiene el contenido binario del archivo PDF de la factura.
     * * @param orderId Identificador de la orden.
     * @return Arreglo de bytes del archivo.
     * @throws IOException Si ocurre un error en la lectura del archivo.
     */
    public byte[] getInvoicePdf(Integer orderId) throws IOException {
        Invoice invoice = getInvoiceForOrder(orderId);
        return Files.readAllBytes(Paths.get(invoice.getPdfUrl()));
    }

    /**
     * Convierte la entidad factura en un DTO simplificado para respuestas de la API.
     * * @param invoice Entidad de la factura.
     * @return InvoiceResponseDTO con el resumen de la factura.
     */
    public InvoiceResponseDTO convertInvoiceToResponseDTO(Invoice invoice) {
        Orders order = invoice.getOrder();

        List<OrderDetailDTO> details = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            details = order.getOrderDetails().stream().map(item -> {
                OrderDetailDTO d = new OrderDetailDTO();
                d.setProductId(item.getProduct() != null ? item.getProduct().getId() : 0);
                d.setProductName(item.getProduct() != null ? item.getProduct().getName() : "N/A");
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setSubtotal(item.getSubtotal());
                d.setTax(item.getTax());
                d.setTotal(item.getTotal());
                return d;
            }).collect(Collectors.toList());
        }

        return new InvoiceResponseDTO(
                (order.getUser() != null) ? order.getUser().getEmail() : "N/A",
                order.getFinalTotal(),
                order.getId(),
                invoice.getInvoiceNumber(),
                invoice.getDate(),
                details
        );
    }
}