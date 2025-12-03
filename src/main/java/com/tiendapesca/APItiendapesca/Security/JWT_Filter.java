package com.tiendapesca.APItiendapesca.Security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;



/**
 * Filtro JWT que intercepta cada solicitud HTTP para validar Tokens JWT.
 * Este filtro se ejecuta una vez por cada solicitud (OncePerRequestFilter) y:
 *   Extrae el token JWT del header Authorization
 *   Valida el token usando JWT_TokenUtil
 *   Si es válido, establece la autenticación en el SecurityContext
 * Componentes inyectados:
 * - jwtTokenUtil: Utilidades para manejar tokens JWT
 * - customUserDetailsService: Servicio para cargar UserDetails por username
 */
@Component
public class JWT_Filter extends OncePerRequestFilter {

    private final JWT_TokenUtil jwtTokenUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public JWT_Filter(JWT_TokenUtil jwtTokenUtil, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        
        //Obtener el header Authorization
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        //Extraer el token JWT (formato: "Bearer <token>")
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Elimina "Bearer " para obtener solo el token
            username = jwtTokenUtil.extractUsername(jwt); // Extrae el username del token
        }

        // Validar token y configurar autenticación
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Cargar UserDetails desde la base de datos
            UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);
            
            // Verificar validez del token contra los UserDetails
            if (jwtTokenUtil.validateToken(jwt, userDetails)) {
              
                UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, 
                        userDetails.getAuthorities()); // Roles/permisos
                
                // Añadir detalles de la solicitud (IP, sessionId, etc.)
                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Establecer autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        
        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}