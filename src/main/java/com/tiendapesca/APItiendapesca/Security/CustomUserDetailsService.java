package com.tiendapesca.APItiendapesca.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tiendapesca.APItiendapesca.Service.Users_Service;

/**
 * Servicio personalizado para cargar detalles de usuario durante la autenticación.
 * 
 * Implementa la interfaz UserDetailsService de Spring Security para:
 * - Conectar el sistema de autenticación con tu modelo de usuarios personalizado
 * - Cargar los detalles del usuario desde tu base de datos
 * 
 * Inyecta el Users_Service de la aplicación para acceder a los usuarios registrados
 */
@Service 
public class CustomUserDetailsService implements UserDetailsService {

    private final Users_Service usersService; 

    
    public CustomUserDetailsService(Users_Service usersService) {
        this.usersService = usersService;
    }

    /**
     * Método principal que carga un usuario por su nombre de usuario (Email)
     * @param username El email que sirve como nombre de usuario
     * @return UserDetails con la información requerida por Spring Security
     * @throws UsernameNotFoundException Si el usuario no existe
     
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
     
        return usersService.findByEmail(username); 
    }
}