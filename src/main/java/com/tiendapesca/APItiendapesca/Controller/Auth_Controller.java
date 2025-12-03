package com.tiendapesca.APItiendapesca.Controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tiendapesca.APItiendapesca.Entities.Users;
import com.tiendapesca.APItiendapesca.Security.JWT_TokenUtil;
import com.tiendapesca.APItiendapesca.Service.Users_Service;

/**
 * Controlador REST para manejar operaciones de autenticación y registro de usuarios
 * Expone endpoints para registro y login de usuarios con generación de tokens JWT
 */
@RestController
@RequestMapping("/api/auth")
public class Auth_Controller {

    private final Users_Service userService;
    private final JWT_TokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor para inyección de dependencias
     * @param userService Servicio para operaciones con usuarios
     * @param jwtTokenUtil Utilidad para manejo de tokens JWT
     * @param passwordEncoder Codificador de contraseñas
     */
    public Auth_Controller(Users_Service userService, JWT_TokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Endpoint para registro de nuevos usuarios
     * @param user Objeto Users con los datos del usuario a registrar
     * @return Mensaje de confirmación del registro
     */
    @PostMapping("/register")
    public String register(@RequestBody Users user) {
        userService.register(user);
        return "Usuario registrado correctamente";
    }

    /**
     * Endpoint para autenticación de usuarios existentes
     * @param user Objeto Users con credenciales de login (email y password)
     * @return Map con el token JWT generado
     * @throws RuntimeException si las credenciales son inválidas o el usuario no existe
     */
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Users user) {
        var foundUser = userService.findByEmail(user.getEmail());

        if (foundUser == null) {
            throw new RuntimeException("El usuario no existe");
        }

        if (!passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        String token = jwtTokenUtil.generateToken(foundUser.getEmail());
        return Map.of("token", token);
    }

    /**
     * Manejador de excepciones para RuntimeExceptions
     * @param ex Excepción capturada
     * @return ResponseEntity con mensaje de error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}