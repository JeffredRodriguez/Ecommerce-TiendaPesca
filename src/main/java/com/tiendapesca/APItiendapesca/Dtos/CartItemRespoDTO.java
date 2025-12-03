package com.tiendapesca.APItiendapesca.Dtos;

import java.math.BigDecimal;


//l
public class CartItemRespoDTO {
    private Integer cartItemId;
    private Integer productId;
    private String productName;
    private String productImage;
    private String brand;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    
    public CartItemRespoDTO(Integer cartItemId, Integer productId, String productName, 
                          String productImage, String brand, Integer quantity, 
                          BigDecimal unitPrice) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.brand = brand;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters
    public Integer getCartItemId() { return cartItemId; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImage() { return productImage; }
    public String getBrand() { return brand; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}