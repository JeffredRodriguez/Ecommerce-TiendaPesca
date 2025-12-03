package com.tiendapesca.APItiendapesca.Dtos;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
//l
public class OrderCalculationResult {
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;
    private final Map<Integer, Integer> productQuantities;

    // Constructor principal
    public OrderCalculationResult(BigDecimal subtotal, 
                                BigDecimal tax, 
                                BigDecimal total, 
                                Map<Integer, Integer> productQuantities) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.tax = tax != null ? tax : BigDecimal.ZERO;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.productQuantities = productQuantities != null ? 
            Collections.unmodifiableMap(productQuantities) : 
            Collections.emptyMap();
    }

    // Getters
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Map<Integer, Integer> getProductQuantities() {
        return productQuantities;
    }

    // Método factory estático
    public static OrderCalculationResult of(BigDecimal subtotal, 
                                         BigDecimal taxRate, 
                                         Map<Integer, Integer> productQuantities) {
        BigDecimal tax = subtotal.multiply(taxRate);
        BigDecimal total = subtotal.add(tax);
        return new OrderCalculationResult(subtotal, tax, total, productQuantities);
    }

    // equals() y hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCalculationResult that = (OrderCalculationResult) o;
        return Objects.equals(subtotal, that.subtotal) &&
               Objects.equals(tax, that.tax) &&
               Objects.equals(total, that.total) &&
               Objects.equals(productQuantities, that.productQuantities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subtotal, tax, total, productQuantities);
    }

    // toString()
    @Override
    public String toString() {
        return "OrderCalculationResult{" +
               "subtotal=" + subtotal +
               ", tax=" + tax +
               ", total=" + total +
               ", productQuantities=" + productQuantities +
               '}';
    }

    // Método de conveniencia
    public boolean hasProducts() {
        return !productQuantities.isEmpty();
    }

    // Builder estático
    public static Builder builder() {
        return new Builder();
    }

    // Clase Builder
    public static final class Builder {
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        private Map<Integer, Integer> productQuantities = Collections.emptyMap();

        private Builder() {}

        public Builder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder productQuantities(Map<Integer, Integer> productQuantities) {
            this.productQuantities = productQuantities;
            return this;
        }

        public OrderCalculationResult build() {
            return new OrderCalculationResult(subtotal, tax, total, productQuantities);
        }
    }
}