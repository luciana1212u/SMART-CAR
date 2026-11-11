package com.smartcar.backend.services;

import com.smartcar.backend.dto.SesionResponse;
import com.smartcar.backend.models.Usuario;

/**
 * Autenticacion con Supabase Auth. No hay sesion global: cada peticion se identifica con su propio JWT
 * ({@link #autenticarSolicitud}) y {@link #estaAutenticado()} responde solo por la peticion en curso.
 */
public interface IAuthService {
    Usuario registrar(String nombre, String nombreUsuario, String email, String password);

    /** true/false si el nombre de usuario esta libre; lanza AuthException si no se puede comprobar. */
    boolean usuarioDisponible(String nombreUsuario);

    SesionResponse iniciarSesion(String email, String password);

    SesionResponse refrescar(String refreshToken);

    void cerrarSesion(String accessToken);

    /** Valida el JWT (firma, emisor, audiencia, expiracion), anota la actividad del cliente y devuelve el usuario. */
    Usuario autenticarSolicitud(String accessToken);

    /** true si la peticion HTTP en curso trae un JWT valido. Fuera de una peticion (supervisor) es false. */
    boolean estaAutenticado();

    /** Usuario dueno del JWT de la peticion en curso, o null. */
    Usuario getUsuarioAutenticado();

    /**
     * Parada segura: true mientras la app siga mandando peticiones autenticadas. El supervisor (sin peticion HTTP)
     * detiene el carro cuando deja de serlo.
     */
    default boolean comunicacionVigente() {
        return estaAutenticado();
    }
}
