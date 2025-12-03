package com.tiendapesca.APItiendapesca.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InvoicePdfDTO {
    private String invoiceNumber;
    private LocalDateTime date;
    private String paymentMethod;
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private String phone;
    private List<ProductItemDTO> products;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    
    
    
    
    
    
    public InvoicePdfDTO() {
		super();
	}

	// Constructor
    public InvoicePdfDTO(String invoiceNumber, LocalDateTime date, String paymentMethod,
                        String customerName, String customerEmail, String shippingAddress,
                        String phone, List<ProductItemDTO> products, BigDecimal subtotal,
                        BigDecimal tax, BigDecimal total) {
        this.invoiceNumber = invoiceNumber;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.shippingAddress = shippingAddress;
        this.phone = phone;
        this.products = products;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
    }
    
    // Getters y Setters
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public List<ProductItemDTO> getProducts() { return products; }
    public void setProducts(List<ProductItemDTO> products) { this.products = products; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}