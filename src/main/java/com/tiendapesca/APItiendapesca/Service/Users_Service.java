package com.tiendapesca.APItiendapesca.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Repository.Users_Repository;

/**
 * Servicio para gestionar operaciones relacionadas con usuarios
 * Implementa UserDetailsService para integración con Spring Security
 */
@Service
public class Users_Service implements UserDetailsService {
    
    private final Users_Repository repo;
    private final PasswordEncoder encoder;

    /**
     * Constructor para inyección de Dependencias
     * @param repo Repositorio de usuarios
     * @param encoder Codificador de contraseñas
     */
    public Users_Service(Users_Repository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /**
     * Registra un nuevo usuario en el sistema
     * @param user Usuario a registrar
     * @return Usuario registrado con la contraseña encriptada
     */
    public Users register(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    /**
     * Busca un usuario por su email
     * @param email Email del usuario a buscar
     * @return Usuario encontrado
     * @throws UsernameNotFoundException Si no se encuentra el usuario
     */
    public Users findByEmail(String email) {
        return repo.findByEmail(email).orElseThrow();
    }

    /**
     * Carga los detalles del usuario por email para Spring Security
     * @param email Email del usuario
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException Si no se encuentra el usuario
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el email: " + email));
    }
}