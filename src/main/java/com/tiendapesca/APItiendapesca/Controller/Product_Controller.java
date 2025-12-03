package com.tiendapesca.APItiendapesca.Controller;

import com.tiendapesca.APItiendapesca.Entities.Product;
import com.tiendapesca.APItiendapesca.Service.Product_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de productos.
 * Proporciona endpoints para operaciones CRUD y paginación de resultados.
 */
@RestController
@RequestMapping("/api/products")
public class Product_Controller {

    @Autowired
    private Product_Service productService;
    
    /**
     * Obtiene una lista paginada de productos.
     * @param page número de página (por defecto 0)
     * @param size cantidad de elementos por página (por defecto 10)
     * @return página de productos
     */
    @GetMapping("/get")
    public Page<Product> AllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return productService.AllProducts(pageable);
    }

    /**
     * Crea un nuevo producto en la base de datos.
     * @param product objeto Product con los datos a registrar
     * @return el producto creado con estado HTTP 201 
     */
    @PostMapping("/create")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.saveProduct(product);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /**
     * Actualiza un producto existente.
     * @param id identificador del producto a actualizar
     * @param product objeto Product con los nuevos valores
     * @return el producto actualizado
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable int id, 
            @RequestBody Product product) {

        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina un producto de la base de datos.
     * @param id identificador del producto a eliminar
     * @return mensaje de confirmación o error si no se encuentra el producto
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> eliminarProducto(@PathVariable int id) {
        try {
            productService.deleteProduct(id);  
            return ResponseEntity.ok("Producto eliminado correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}