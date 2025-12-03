package com.tiendapesca.APItiendapesca.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tiendapesca.APItiendapesca.Entities.Users;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con usuarios
 * Extiende JpaRepository para operaciones CRUD básicas
 */
@Repository
public interface Users_Repository extends JpaRepository<Users, Integer> {
    
    /**
     * Busca un usuario por su dirección de correo electrónico
     * @param email Dirección de email del usuario a buscar
     * @return Optional con el usuario si existe, vacío si no se encuentra
     */
    Optional<Users> findByEmail(String email);
}