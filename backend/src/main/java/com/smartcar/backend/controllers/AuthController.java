package com.smartcar.backend.controllers;

import com.smartcar.backend.dto.Peticiones;
import com.smartcar.backend.dto.SesionResponse;
import com.smartcar.backend.dto.UsuarioResponse;
import com.smartcar.backend.models.Usuario;
import com.smartcar.backend.services.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Publicos: registro, login, refresh y usuario-disponible. El resto exige el JWT (ver JwtAuthInterceptor). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody Peticiones.Registro body) {
        Usuario u = authService.registrar(body.nombre(), body.usuario(), body.email(), body.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(u));
    }

    @GetMapping("/usuario-disponible")
    public Map<String, Object> usuarioDisponible(@RequestParam("usuario") String usuario) {
        return Map.of("disponible", authService.usuarioDisponible(usuario));
    }

    @PostMapping("/login")
    public SesionResponse iniciarSesion(@RequestBody Peticiones.Login body) {
        return authService.iniciarSesion(body.email(), body.password());
    }

    @PostMapping("/refresh")
    public SesionResponse renovar(@RequestBody Peticiones.Refresco body) {
        return authService.refrescar(body.refreshToken());
    }

    @PostMapping("/logout")
    public Map<String, Object> cerrarSesion(@RequestHeader("Authorization") String autorizacion) {
        authService.cerrarSesion(autorizacion.substring(7).trim());
        return Map.of("autenticado", false);
    }

    @GetMapping("/sesion")
    public Map<String, Object> verificarSesion() {
        return Map.of("autenticado", true, "usuario", UsuarioResponse.de(authService.getUsuarioAutenticado()));
    }
}
