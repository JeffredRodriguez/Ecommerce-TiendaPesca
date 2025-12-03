package com.tiendapesca.APItiendapesca.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore; // ← IMPORTANTE: Agregar esta importación
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ✅ CAMBIO CRÍTICO: Agregar @JsonIgnore para romper el bucle
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    @JsonIgnore // ← ESTO EVITA EL BUCLE: Invoice -> Order -> Invoice -> Order...
    private Orders order;

    @Column(name = "date", columnDefinition = "DATETIME")
    private LocalDateTime date;

    @Column(name = "invoice_number", length = 50, unique = true)
    private String invoiceNumber;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    // Nuevos campos para cancelación
    @Column(name = "is_canceled", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCanceled = false;

    @Column(name = "cancelation_date", columnDefinition = "DATETIME")
    private LocalDateTime cancelationDate;

    // --- Constructores ---
    public Invoice() {
        this.date = LocalDateTime.now();
        this.isCanceled = false;
    }

    // Constructor sin la referencia circular en parámetros
    public Invoice(LocalDateTime date, String invoiceNumber, String pdfUrl,
                  Boolean isCanceled, LocalDateTime cancelationDate) {
        this.date = date != null ? date : LocalDateTime.now();
        this.invoiceNumber = invoiceNumber;
        this.pdfUrl = pdfUrl;
        this.isCanceled = isCanceled != null ? isCanceled : false;
        this.cancelationDate = cancelationDate;
    }

    // Constructor completo (para casos donde necesites el order)
    public Invoice(Integer id, Orders order, LocalDateTime date, String invoiceNumber, String pdfUrl,
                  Boolean isCanceled, LocalDateTime cancelationDate) {
        this.id = id;
        this.order = order;
        this.date = date != null ? date : LocalDateTime.now();
        this.invoiceNumber = invoiceNumber;
        this.pdfUrl = pdfUrl;
        this.isCanceled = isCanceled != null ? isCanceled : false;
        this.cancelationDate = cancelationDate;
    }

    // --- Getters y Setters ---
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Orders getOrder() {
        return order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Boolean getIsCanceled() {
        return isCanceled;
    }

    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    public LocalDateTime getCancelationDate() {
        return cancelationDate;
    }

    public void setCancelationDate(LocalDateTime cancelationDate) {
        this.cancelationDate = cancelationDate;
    }

    // --- Métodos utilitarios ---
    
    /**
     * Cancela la factura y establece la fecha de cancelación
     */
    public void cancel() {
        this.isCanceled = true;
        this.cancelationDate = LocalDateTime.now();
    }
    
    /**
     * Verifica si la factura está activa (no cancelada)
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(isCanceled);
    }
    
    /**
     * Método toString seguro que evita referencias circulares
     */
    @Override
    public String toString() {
        return String.format(
            "Invoice{id=%d, invoiceNumber='%s', date=%s, pdfUrl=%s, isCanceled=%s}",
            id,
            invoiceNumber != null ? invoiceNumber : "null",
            date != null ? date.toString() : "null",
            pdfUrl != null ? "'" + pdfUrl + "'" : "null",
            isCanceled
        );
    }
    
    /**
     * Método para validar que la factura tenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return invoiceNumber != null && !invoiceNumber.trim().isEmpty() &&
               date != null && order != null;
    }
}