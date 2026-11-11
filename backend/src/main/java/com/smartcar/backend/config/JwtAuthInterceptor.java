package com.smartcar.backend.config;

import com.smartcar.backend.excepciones.AuthException;
import com.smartcar.backend.models.Usuario;
import com.smartcar.backend.services.AuthService;
import com.smartcar.backend.services.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Cada peticion protegida debe traer su propio "Authorization: Bearer <JWT de Supabase>".
 * Sin token valido responde 401 antes de llegar al controlador, asi que ningun cliente sin sesion puede mover el carro.
 */
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final IAuthService auth;

    public JwtAuthInterceptor(IAuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;   // preflight CORS
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw AuthException.tokenInvalido();
        }
        Usuario usuario = auth.autenticarSolicitud(cabecera.substring(7).trim());
        request.setAttribute(AuthService.ATRIBUTO_USUARIO, usuario);
        return true;
    }
}
