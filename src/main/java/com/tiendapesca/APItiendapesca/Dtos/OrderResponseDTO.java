package com.tiendapesca.APItiendapesca.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.tiendapesca.APItiendapesca.Entities.OrderStatus;
import com.tiendapesca.APItiendapesca.Entities.PaymentMethod;

public class OrderResponseDTO {
    private Integer orderId;
    private LocalDateTime orderDate;
    private String shippingAddress;
    private String phone;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private UserDTO user;
    private List<OrderDetailDTO> orderDetails;

    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private String pdfUrl; // URL para descargar el PDF


    public OrderResponseDTO() {
    }

    public OrderResponseDTO(Integer orderId, LocalDateTime orderDate, String shippingAddress,
                            String phone, BigDecimal subtotal, BigDecimal tax,
                            BigDecimal total, PaymentMethod paymentMethod,
                            OrderStatus status, UserDTO user, List<OrderDetailDTO> orderDetails,
                            String invoiceNumber, LocalDateTime invoiceDate, String pdfUrl) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.shippingAddress = shippingAddress;
        this.phone = phone;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.user = user;
        this.orderDetails = orderDetails;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.pdfUrl = pdfUrl;
    }


    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public List<OrderDetailDTO> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetailDTO> orderDetails) { this.orderDetails = orderDetails; }


    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}