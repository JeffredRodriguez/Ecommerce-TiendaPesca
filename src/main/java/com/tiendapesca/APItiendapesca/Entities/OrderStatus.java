package com.tiendapesca.APItiendapesca.Entities;

public enum OrderStatus {
	  PROCESSING,
	    COMPLETED,
	    CANCELLED;
	    
	    // Opcional: método para convertir desde String (útil para recibir parámetros)
	    public static OrderStatus fromString(String value) {
	        try {
	            return OrderStatus.valueOf(value.toUpperCase());
	        } catch (IllegalArgumentException e) {
	            throw new IllegalArgumentException("Estado de orden no válido: " + value + 
	                ". Los valores válidos son: PROCESSING, COMPLETED, CANCELLED");
	        }
	    }
}
