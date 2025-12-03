package com.tiendapesca.APItiendapesca.Controller;

import com.tiendapesca.APItiendapesca.Dtos.InvoiceResponseDTO;
import com.tiendapesca.APItiendapesca.Entities.Invoice;
import com.tiendapesca.APItiendapesca.Service.Invoice_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controlador REST para gestionar operaciones relacionadas con facturas
 * Proporciona endpoints para generar, consultar y gestionar facturas
 */
@RestController
@RequestMapping("/api/invoices")
public class Invoice_Controller {

    private final Invoice_Service invoiceService;

    /**
     * Constructor para inyección de dependencias del servicio de facturas
     */
    @Autowired
    public Invoice_Controller(Invoice_Service invoiceService) {
        this.invoiceService = invoiceService;
    }
    
    /**
     * Genera una factura para una orden específica
     * @param orderId ID de la orden para la cual generar la factura
     * @return ResponseEntity con la factura generada o mensaje de error
     */
    @PostMapping("/generate/{orderId}")
    public ResponseEntity<?> generateInvoice(@PathVariable Integer orderId) {
        try {
            Invoice invoice = invoiceService.generateAndSaveInvoice(orderId);
            return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar factura: " + e.getMessage());
        }
    }

    /**
     * Obtiene la factura asociada a una orden específica
     * @param orderId ID de la orden a consultar
     * @return ResponseEntity con los datos de la factura o mensaje de error
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getInvoiceByOrder(@PathVariable Integer orderId) {
        try {
            Invoice invoice = invoiceService.getInvoiceForOrder(orderId);
            InvoiceResponseDTO responseDTO = invoiceService.convertInvoiceToResponseDTO(invoice);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener factura: " + e.getMessage());
        }
    }

    /**
     * Obtiene el archivo PDF de una factura específica
     * @param orderId ID de la orden asociada a la factura
     * @return ResponseEntity con el archivo PDF o mensaje de error
     */
    @GetMapping("/{orderId}/pdf")
    public ResponseEntity<?> getInvoicePdf(@PathVariable Integer orderId) {
        try {
            byte[] pdfBytes = invoiceService.getInvoicePdf(orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "factura_" + orderId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al leer el archivo PDF: " + e.getMessage());
        }
    }

    /**
     * Envía la factura por correo electrónico al cliente
     * @param orderId ID de la orden asociada a la factura
     * @param email Dirección de correo electrónico del destinatario
     * @return ResponseEntity con confirmación de envío o mensaje de error
     */
    @PostMapping("/{orderId}/send")
    public ResponseEntity<?> sendInvoiceByEmail(
            @PathVariable Integer orderId,
            @RequestParam String email) {
        try {
            invoiceService.sendInvoiceByEmail(orderId, email);
            return ResponseEntity.ok("Factura enviada exitosamente a " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar factura: " + e.getMessage());
        }
    }

    /**
     * Cancela una factura existente
     * @param orderId ID de la orden asociada a la factura a cancelar
     * @return ResponseEntity con confirmación de cancelación o mensaje de error
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelInvoice(@PathVariable Integer orderId) {
        try {
            invoiceService.cancelInvoice(orderId);
            return ResponseEntity.ok("Factura cancelada exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cancelar factura: " + e.getMessage());
        }
    }

    /**
     * Obtiene los detalles completos de una factura
     * @param orderId ID de la orden asociada a la factura
     * @return ResponseEntity con los detalles de la factura o mensaje de error
     */
    @GetMapping("/{orderId}/details")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Integer orderId) {
        try {
            Invoice invoice = invoiceService.getInvoiceForOrder(orderId);
            InvoiceResponseDTO responseDTO = invoiceService.convertInvoiceToResponseDTO(invoice);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener detalles de factura: " + e.getMessage());
        }
    }
}