package com.tiendapesca.APItiendapesca.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tiendapesca.APItiendapesca.Dtos.CartItemRespoDTO;
import com.tiendapesca.APItiendapesca.Entities.Cart;
import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Entities.Users;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con el carrito de compras
 * Extiende JpaRepository para operaciones CRUD básicas
 */
@Repository
public interface Cart_Repository extends JpaRepository<Cart, Integer> {

    /**
     * Consulta personalizada para obtener los items del carrito de un usuario específico
     * @param userId ID del usuario
     * @return Lista de DTOs con la información de los items del carrito
     */
    @Query("SELECT new com.tiendapesca.APItiendapesca.Dtos.CartItemRespoDTO(" +
           "c.id, p.id, p.name, p.image_url, p.brand, c.quantity, p.price) " +
           "FROM Cart c JOIN c.product p WHERE c.user.id = :userId")
    List<CartItemRespoDTO> findCartItemsByUserId(Integer userId);
    
    /**
     * Busca un item específico del carrito por usuario y producto
     * @param user Usuario dueño del carrito
     * @param product Producto a buscar
     * @return Optional con el item del carrito si existe
     */
    Optional<Cart> findByUserAndProduct(Users user, Product product);
    
    /**
     * Elimina todos los items del carrito de un usuario específico
     * @param user Usuario dueño del carrito
     */
    void deleteByUser(Users user);
    
    /**
     * Elimina todos los items del carrito por ID de usuario
     * @param userId ID del usuario
     */
    void deleteByUserId(Integer userId);
    
    /**
     * Encuentra todos los items del carrito de un usuario específico
     * @param user Usuario dueño del carrito
     * @return Lista de items del carrito
     */
    List<Cart> findByUser(Users user);
}