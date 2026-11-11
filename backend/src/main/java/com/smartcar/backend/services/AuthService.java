package com.smartcar.backend.services;

import com.smartcar.backend.dto.SesionResponse;
import com.smartcar.backend.dto.UsuarioResponse;
import com.smartcar.backend.excepciones.AuthException;
import com.smartcar.backend.models.Usuario;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registro y login delegados en Supabase Auth; los JWT se validan en cada peticion.
 * No guarda usuarios ni contrasenas: persisten en Supabase (auth.users + public.perfiles).
 */
public class AuthService implements IAuthService {
    /** Atributo de la peticion HTTP donde el interceptor deja al usuario del JWT. */
    public static final String ATRIBUTO_USUARIO = "smartcar.usuario";

    private static final Pattern USUARIO = Pattern.compile("[a-z0-9_]{3,20}");
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MIN_PASSWORD = 8;

    private final SupabaseAuthClient supabase;
    private final JwtValidator validador;       // null si Supabase no esta configurado
    private final long silencioMs;
    private final Clock reloj;
    private volatile long ultimaActividad;      // marca de tiempo del ultimo JWT valido (0 = ninguna)

    public AuthService(SupabaseAuthClient supabase, JwtValidator validador, long silencioMs, Clock reloj) {
        this.supabase = supabase;
        this.validador = validador;
        this.silencioMs = silencioMs;
        this.reloj = reloj;
    }

    @Override
    public Usuario registrar(String nombre, String nombreUsuario, String email, String password) {
        if (nombre == null || nombre.isBlank() || nombreUsuario == null || nombreUsuario.isBlank()
                || email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Nombre, usuario, correo y contrasena son obligatorios");
        }
        String usuario = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        String correo = email.trim().toLowerCase(Locale.ROOT);
        if (!USUARIO.matcher(usuario).matches()) {
            throw new IllegalArgumentException(
                    "El usuario debe tener de 3 a 20 caracteres: letras minusculas, numeros o guion bajo");
        }
        if (!CORREO.matcher(correo).matches()) throw new IllegalArgumentException("Correo electronico invalido");
        if (password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("La contrasena debe tener al menos " + MIN_PASSWORD + " caracteres");
        }
        // Comprobacion previa (si la funcion SQL existe); el respaldo esta en SupabaseAuthClient.registrar
        if (Boolean.FALSE.equals(supabase.usuarioDisponible(usuario))) throw AuthException.usuarioDuplicado();
        supabase.registrar(nombre.trim(), usuario, correo, password);
        return new Usuario("", nombre.trim(), usuario, correo);
    }

    @Override
    public boolean usuarioDisponible(String nombreUsuario) {
        String usuario = nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (!USUARIO.matcher(usuario).matches()) {
            throw new IllegalArgumentException(
                    "El usuario debe tener de 3 a 20 caracteres: letras minusculas, numeros o guion bajo");
        }
        Boolean libre = supabase.usuarioDisponible(usuario);
        if (libre == null) {
            throw AuthException.supabaseNoDisponible(
                    "No se pudo comprobar el usuario (falta ejecutar supabase/001_auth_smartcar.sql o no hay conexion)");
        }
        return libre;
    }

    @Override
    public SesionResponse iniciarSesion(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Correo y contrasena son obligatorios");
        }
        return aSesion(supabase.iniciarSesion(email.trim().toLowerCase(Locale.ROOT), password));
    }

    @Override
    public SesionResponse refrescar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw AuthException.sesionExpirada();
        return aSesion(supabase.refrescar(refreshToken));
    }

    @Override
    public void cerrarSesion(String accessToken) {
        supabase.cerrarSesion(accessToken);
        // Sin peticiones no hay comunicacion vigente: el supervisor detiene el carro si seguia en marcha
        ultimaActividad = 0;
    }

    @Override
    public Usuario autenticarSolicitud(String accessToken) {
        if (validador == null) throw AuthException.noConfigurado();
        Usuario usuario = validador.validar(accessToken);
        ultimaActividad = reloj.millis();
        return usuario;
    }

    @Override
    public boolean estaAutenticado() {
        return getUsuarioAutenticado() != null;
    }

    @Override
    public Usuario getUsuarioAutenticado() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) return null;
        Object u = atributos.getAttribute(ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
        return u instanceof Usuario usuario ? usuario : null;
    }

    @Override
    public boolean comunicacionVigente() {
        long ultima = ultimaActividad;
        return ultima > 0 && reloj.millis() - ultima <= silencioMs;
    }

    private SesionResponse aSesion(SupabaseAuthClient.Tokens t) {
        if (t.accessToken().isEmpty() || t.refreshToken().isEmpty()) {
            throw AuthException.supabaseNoDisponible("Supabase devolvio una sesion incompleta");
        }
        Usuario u = new Usuario(t.id(), t.nombre(), t.usuario(), t.email());
        return new SesionResponse(t.accessToken(), t.refreshToken(), t.expiresIn(), UsuarioResponse.de(u));
    }
}
