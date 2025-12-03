package com.tiendapesca.APItiendapesca.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//l
public class InvoiceResponseDTO {
    private String userEmail;
    private BigDecimal total;
    private Integer orderId;
    private String invoiceNumber;
    private LocalDateTime date;
    private List<OrderDetailDTO> orderDetails;

    // Constructor
    public InvoiceResponseDTO(String userEmail, BigDecimal total, Integer orderId, String invoiceNumber, LocalDateTime date, List<OrderDetailDTO> orderDetails) {
        this.userEmail = userEmail;
        this.total = total;
        this.orderId = orderId;
        this.invoiceNumber = invoiceNumber;
        this.date = date;
        this.orderDetails = orderDetails;
    }

   

	// Getters y setters
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public List<OrderDetailDTO> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<OrderDetailDTO> orderDetails) {
        this.orderDetails = orderDetails;
    }
}
