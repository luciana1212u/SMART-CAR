package com.smartcar.backend.controllers;

import com.smartcar.backend.dto.Peticiones;
import com.smartcar.backend.dto.UsuarioResponse;
import com.smartcar.backend.models.Usuario;
import com.smartcar.backend.services.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody Peticiones.Registro body) {
        Usuario u = authService.registrar(body.nombre(), body.usuario(), body.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(u));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> iniciarSesion(@RequestBody Peticiones.Login body) {
        boolean ok = authService.iniciarSesion(body.usuario(), body.password());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "CREDENCIALES", "mensaje", "Usuario o contrasena incorrectos"));
        }
        return ResponseEntity.ok(Map.of("autenticado", true,
                "usuario", UsuarioResponse.de(authService.getUsuarioAutenticado())));
    }

    @PostMapping("/logout")
    public Map<String, Object> cerrarSesion() {
        authService.cerrarSesion();
        return Map.of("autenticado", false);
    }

    @GetMapping("/sesion")
    public Map<String, Object> verificarSesion() {
        boolean activa = authService.estaAutenticado();
        return activa
                ? Map.of("autenticado", true, "usuario", UsuarioResponse.de(authService.getUsuarioAutenticado()))
                : Map.of("autenticado", false);
    }
}
