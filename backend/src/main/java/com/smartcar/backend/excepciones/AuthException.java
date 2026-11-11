package com.smartcar.backend.excepciones;

import org.springframework.http.HttpStatus;

/** Error de autenticacion/registro con el codigo HTTP y el codigo de error que entiende la app. */
public class AuthException extends RuntimeException {
    private final HttpStatus estado;
    private final String codigo;

    public AuthException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() { return estado; }

    public String getCodigo() { return codigo; }

    public static AuthException credenciales() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "CREDENCIALES", "Correo o contrasena incorrectos");
    }

    public static AuthException tokenInvalido() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "SIN_SESION", "Sesion invalida. Inicia sesion otra vez");
    }

    public static AuthException sesionExpirada() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "SESION_EXPIRADA", "Tu sesion expiro. Inicia sesion otra vez");
    }

    public static AuthException usuarioDuplicado() {
        return new AuthException(HttpStatus.CONFLICT, "USUARIO_DUPLICADO", "Ese nombre de usuario ya esta en uso");
    }

    public static AuthException correoDuplicado() {
        return new AuthException(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo");
    }

    public static AuthException supabaseNoDisponible(String detalle) {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "SUPABASE_NO_DISPONIBLE", detalle);
    }

    public static AuthException noConfigurado() {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_NO_CONFIGURADA",
                "El backend no tiene configurado Supabase (SUPABASE_URL / SUPABASE_ANON_KEY)");
    }
}
