package com.tiendapesca.APItiendapesca.Dtos;

import com.tiendapesca.APItiendapesca.Entities.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

//l
public class OrderRequestDTO {

    // No puede ser nulo ni vacío ni espacios
    @NotBlank(message = "La dirección de envío es requerida")
    private String shippingAddress;

    // No puede ser nulo ni vacío y debe cumplir el patrón de números
    @NotBlank(message = "El teléfono es requerido")
    @Pattern(
        regexp = "^[0-9]{8,20}$",
        message = "El teléfono debe contener solo números y tener entre 8 y 20 dígitos"
    )
    private String phone;

    // No puede ser nulo
    @NotNull(message = "El método de pago es requerido")
    private PaymentMethod paymentMethod;

    // Getters y Setters
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
