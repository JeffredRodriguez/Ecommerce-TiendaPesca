package com.tiendapesca.APItiendapesca.Controller;

import com.tiendapesca.APItiendapesca.Dtos.AddToCartRequestDTO;
import com.tiendapesca.APItiendapesca.Dtos.CartItemRespoDTO;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Service.Cart_Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para gestionar operaciones del carrito de compras.
 * Proporciona endpoints para agregar, consultar, actualizar y eliminar items del carrito.
 */
@RestController
@RequestMapping("/api/cart")
public class Cart_Controller {

    private final Cart_Service cartService;

    /**
     * Constructor para inyección de dependencias del servicio de carrito.
     * @param cartService Servicio para operaciones del carrito
     */
    @Autowired
    public Cart_Controller(Cart_Service cartService) {
        this.cartService = cartService;
    }

    /**
     * Agrega productos al carrito de compras del usuario autenticado.
     * @param user Usuario autenticado (inyectado automáticamente)
     * @param request DTO con los datos del producto a agregar
     * @return ResponseEntity con estado 201 (CREATED)
     */
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(@AuthenticationPrincipal Users user,
                                          @Valid @RequestBody AddToCartRequestDTO request) {
        cartService.addProductToCart(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Obtiene todos los items del carrito del usuario autenticado.
     * @param user Usuario autenticado
     * @return Lista de DTOs con los items del carrito
     */
    @GetMapping("/get")
    public ResponseEntity<List<CartItemRespoDTO>> getCart(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(cartService.getCartItems(user.getId()));
    }

    /**
     * Calcula el total del pedido en el carrito del usuario.
     * @param user Usuario autenticado
     * @return Total del carrito como BigDecimal
     */
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getCartTotal(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(cartService.calculateCartTotal(user));
    }

    /**
     * Actualiza la cantidad de un item específico en el carrito.
     * @param user Usuario autenticado
     * @param cartItemId ID del item del carrito a actualizar
     * @param quantity Nueva cantidad (mínimo 1)
     * @return ResponseEntity con estado 204 (NO CONTENT)
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateCartItem(@AuthenticationPrincipal Users user,
                                               @PathVariable Integer cartItemId,
                                               @RequestParam @Min(1) Integer quantity) {
        cartService.updateCartItemQuantity(user, cartItemId, quantity);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina un item específico del carrito.
     * @param user Usuario autenticado
     * @param cartItemId ID del item a eliminar
     * @return ResponseEntity con estado 204 (NO CONTENT)
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(@AuthenticationPrincipal Users user,
                                               @PathVariable Integer cartItemId) {
        cartService.removeCartItem(user, cartItemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Limpia todos los items del carrito del usuario.
     * @param user Usuario autenticado
     * @return ResponseEntity con estado 204 (NO CONTENT)
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Users user) {
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
