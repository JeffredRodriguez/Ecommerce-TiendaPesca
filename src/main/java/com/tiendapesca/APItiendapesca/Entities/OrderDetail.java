package com.tiendapesca.APItiendapesca.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore; // ← IMPORTANTE: Agregar esta importación
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_detail")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ✅ CAMBIO CRÍTICO: Agregar @JsonIgnore para romper el bucle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore // ← ESTO EVITA EL BUCLE: OrderDetail -> Order -> OrderDetails -> Order...
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    // --- Constructores ---
    public OrderDetail() {}

    // Constructor sin la referencia circular en parámetros
    public OrderDetail(Product product, Integer quantity, 
                      BigDecimal unitPrice, BigDecimal subtotal, 
                      BigDecimal tax, BigDecimal total) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    // --- Métodos utilitarios ---
    
    /**
     * Método para calcular automáticamente los valores
     */
    public void calculateValues() {
        if (this.unitPrice != null && this.quantity != null) {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
            
            // Calcular impuesto (13% por ejemplo)
            if (this.tax == null) {
                this.tax = this.subtotal.multiply(BigDecimal.valueOf(0.13));
            }
            
            this.total = this.subtotal.add(this.tax != null ? this.tax : BigDecimal.ZERO);
        }
    }
    
    /**
     * Método toString seguro que evita referencias circulares
     */
    @Override
    public String toString() {
        return String.format(
            "OrderDetail{id=%d, productId=%s, quantity=%d, unitPrice=%.2f, subtotal=%.2f}",
            id,
            product != null ? product.getId() : "null",
            quantity,
            unitPrice != null ? unitPrice : BigDecimal.ZERO,
            subtotal != null ? subtotal : BigDecimal.ZERO
        );
    }
}