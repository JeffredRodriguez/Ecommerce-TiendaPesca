package com.tiendapesca.APItiendapesca.Dtos;

// Dto de carrito
public class AddToCartRequestDTO {
    private Integer productId;
    private Integer quantity;

    // Constructor, getters y setters
    public AddToCartRequestDTO() {
    }

    public AddToCartRequestDTO(Integer productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}