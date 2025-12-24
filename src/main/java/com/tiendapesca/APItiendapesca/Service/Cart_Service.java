package com.tiendapesca.APItiendapesca.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tiendapesca.APItiendapesca.Dtos.AddToCartRequestDTO;
import com.tiendapesca.APItiendapesca.Dtos.CartItemRespoDTO;
import com.tiendapesca.APItiendapesca.Entities.Cart;
import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Repository.Cart_Repository;
import com.tiendapesca.APItiendapesca.Repository.Product_Repository;
import com.tiendapesca.APItiendapesca.Repository.Users_Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la logica de negocio del carrito de compras,
 * gestionando la persistencia, validacion de stock y pertenencia de items.
 */
@Service
public class Cart_Service {

    private final Cart_Repository cartRepository;
    private final Product_Repository productRepository;
    private final Users_Repository userRepository;

    @Autowired
    public Cart_Service(Cart_Repository cartRepository,
                        Product_Repository productRepository,
                        Users_Repository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * Agrega un producto al carrito. Si el producto ya existe para el usuario,
     * incrementa la cantidad actual. Valida disponibilidad de stock antes de guardar.
     * * @param user Usuario autenticado.
     * @param request DTO con el ID del producto y la cantidad a añadir.
     */
    @Transactional
    public void addProductToCart(Users user, AddToCartRequestDTO request) {
        validateUserAndRequest(user, request);
        Product product = getProductById(request.getProductId());

        Optional<Cart> existingCartItem = cartRepository.findByUserAndProduct(user, product);

        if (existingCartItem.isPresent()) {
            updateExistingCartItem(existingCartItem.get(), request.getQuantity(), product);
        } else {
            validateStock(product, request.getQuantity());
            createNewCartItem(user, product, request.getQuantity());
        }
    }

    /**
     * Obtiene la lista de items en el carrito para un usuario especifico.
     * * @param userId Identificador del usuario.
     * @return Lista de DTOs con la informacion detallada del carrito.
     */
    public List<CartItemRespoDTO> getCartItems(Integer userId) {
        validateUserExists(userId);
        return cartRepository.findCartItemsByUserId(userId);
    }

    /**
     * Actualiza la cantidad de un item del carrito. Si la cantidad es menor o igual a cero,
     * el item es eliminado.
     * * @param user Usuario autenticado.
     * @param cartItemId Identificador del item en la tabla cart.
     * @param quantity Nueva cantidad deseada.
     */
    @Transactional
    public void updateCartItemQuantity(Users user, Integer cartItemId, Integer quantity) {
        Cart cartItem = getCartItemById(cartItemId);
        validateUserOwnership(user, cartItem);

        if (quantity <= 0) {
            cartRepository.delete(cartItem);
        } else {
            updateItemQuantity(cartItem, quantity);
        }
    }

    /**
     * Elimina un item especifico del carrito verificando que pertenezca al usuario.
     * * @param user Usuario autenticado.
     * @param cartItemId Identificador del item a eliminar.
     */
    @Transactional
    public void removeCartItem(Users user, Integer cartItemId) {
        Cart cartItem = getCartItemById(cartItemId);
        validateUserOwnership(user, cartItem);
        cartRepository.delete(cartItem);
    }

    /**
     * Elimina todos los registros del carrito asociados a un usuario.
     * * @param user Usuario autenticado.
     */
    @Transactional
    public void clearCart(Users user) {
        validateUser(user);
        cartRepository.deleteByUser(user);
    }

    /**
     * Realiza la sumatoria de subtotales (precio * cantidad) de todos los productos
     * presentes en el carrito del usuario.
     * * @param user Usuario autenticado.
     * @return Suma total como BigDecimal.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(Users user) {
        validateUser(user);
        return cartRepository.findCartItemsByUserId(user.getId())
                .stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- Metodos Auxiliares ---

    /**
     * Valida que el usuario este presente y la cantidad sea positiva.
     */
    private void validateUserAndRequest(Users user, AddToCartRequestDTO request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autenticado");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a cero");
        }
    }

    /**
     * Busca un producto por ID o lanza excepcion 404.
     */
    private Product getProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /**
     * Verifica si la cantidad solicitada supera el stock disponible del producto.
     */
    private void validateStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("No hay suficiente stock para %s. Stock disponible: %d",
                            product.getName(), product.getStock()));
        }
    }

    /**
     * Actualiza la cantidad de un registro existente sumando la cantidad adicional.
     */
    private void updateExistingCartItem(Cart cartItem, int additionalQuantity, Product product) {
        int newQuantity = cartItem.getQuantity() + additionalQuantity;
        validateStock(product, newQuantity);
        cartItem.setQuantity(newQuantity);
        cartRepository.save(cartItem);
    }

    /**
     * Crea e inserta un nuevo registro en la tabla de carrito.
     */
    private void createNewCartItem(Users user, Product product, int quantity) {
        Cart newCartItem = new Cart();
        newCartItem.setUser(user);
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        cartRepository.save(newCartItem);
    }

    /**
     * Valida la existencia de un usuario en la base de datos.
     */
    private void validateUserExists(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
    }

    /**
     * Recupera un item del carrito por ID o lanza excepcion 404.
     */
    private Cart getCartItemById(Integer cartItemId) {
        return cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item del carrito no encontrado"));
    }

    /**
     * Verifica que el item del carrito pertenezca al usuario que intenta modificarlo.
     */
    private void validateUserOwnership(Users user, Cart cartItem) {
        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar este item del carrito");
        }
    }

    /**
     * Valida la presencia de un objeto usuario.
     */
    private void validateUser(Users user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autenticado");
        }
    }

    /**
     * Actualiza la cantidad de un item validando el stock del producto asociado.
     */
    private void updateItemQuantity(Cart cartItem, int quantity) {
        validateStock(cartItem.getProduct(), quantity);
        cartItem.setQuantity(quantity);
        cartRepository.save(cartItem);
    }
}