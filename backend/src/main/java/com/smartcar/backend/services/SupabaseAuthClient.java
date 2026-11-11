package com.smartcar.backend.services;

import com.smartcar.backend.excepciones.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP de Supabase Auth (GoTrue) y PostgREST. Solo usa la clave publicable (sb_publishable_...) o la
 * anon clasica (JWT); nunca service_role.
 */
public class SupabaseAuthClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthClient.class);

    /** Respuesta de /token: tokens mas los datos basicos del usuario. */
    public record Tokens(String accessToken, String refreshToken, long expiresIn,
                         String id, String email, String nombre, String usuario) { }

    private final String url;
    private final String anonKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public SupabaseAuthClient(String url, String anonKey) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.anonKey = anonKey == null ? "" : anonKey.trim();
    }

    public boolean estaConfigurado() {
        return !url.isEmpty() && !anonKey.isEmpty();
    }

    public String getUrl() {
        return url;
    }

    /** Crea la cuenta. nombre y usuario viajan en data: el trigger al_crear_usuario los lee de raw_user_meta_data. */
    public void registrar(String nombre, String usuario, String email, String password) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("email", email);
        cuerpo.put("password", password);
        cuerpo.put("data", Map.of("nombre", nombre, "usuario", usuario));
        Respuesta r = enviar("POST", "/auth/v1/signup", cuerpo, null);
        if (r.estado / 100 == 2) return;

        String codigo = r.texto("error_code");
        String mensaje = r.texto("msg");
        if (mensaje.isEmpty()) mensaje = r.texto("message");
        if (mensaje.isEmpty()) mensaje = r.texto("error_description");
        log.warn("Supabase rechazo el registro: estado={} codigo={} mensaje={}", r.estado, codigo, mensaje);

        if (codigo.equals("user_already_exists") || codigo.equals("email_exists")) {
            throw AuthException.correoDuplicado();
        }
        if (codigo.equals("weak_password")) {
            throw new IllegalArgumentException("La contrasena es muy debil: usa 8 o mas caracteres");
        }
        if (r.estado == 429 || codigo.contains("rate_limit")) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS",
                    "Demasiados registros seguidos. Espera un momento e intenta de nuevo");
        }
        if (r.estado >= 500 && mensaje.toLowerCase().contains("database error")) {
            // El trigger fallo. Lo mas probable es el nombre de usuario unico, pero se comprueba: no se asume.
            Boolean disponible = usuarioDisponible(usuario);
            if (Boolean.FALSE.equals(disponible)) throw AuthException.usuarioDuplicado();
            throw new AuthException(HttpStatus.BAD_GATEWAY, "REGISTRO_FALLIDO",
                    "No se pudo crear la cuenta (error al guardar el perfil en Supabase). Revisa el trigger al_crear_usuario");
        }
        if (r.estado == 400 || r.estado == 422) {
            throw new IllegalArgumentException("Registro invalido: " + (mensaje.isEmpty() ? "revisa los datos" : mensaje));
        }
        throw new AuthException(HttpStatus.BAD_GATEWAY, "REGISTRO_FALLIDO", "Supabase no pudo crear la cuenta");
    }

    /** true/false segun la funcion RPC usuario_disponible; null si no se pudo comprobar. */
    public Boolean usuarioDisponible(String usuario) {
        try {
            Respuesta r = enviar("POST", "/rest/v1/rpc/usuario_disponible", Map.of("p_usuario", usuario), null);
            if (r.estado / 100 == 2 && r.cuerpo != null && r.cuerpo.isBoolean()) return r.cuerpo.asBoolean();
            log.warn("usuario_disponible no respondio un booleano (estado {}). Ejecuta supabase/001_auth_smartcar.sql", r.estado);
        } catch (AuthException e) {
            log.warn("No se pudo comprobar usuario_disponible: {}", e.getMessage());
        }
        return null;
    }

    public Tokens iniciarSesion(String email, String password) {
        Respuesta r = enviar("POST", "/auth/v1/token?grant_type=password",
                Map.of("email", email, "password", password), null);
        if (r.estado == 400 || r.estado == 401 || r.estado == 422) {
            if (r.texto("error_code").equals("email_not_confirmed")) {
                throw new AuthException(HttpStatus.FORBIDDEN, "CORREO_SIN_CONFIRMAR",
                        "Confirma tu correo antes de iniciar sesion");
            }
            throw AuthException.credenciales();
        }
        return aTokens(r, "No se pudo iniciar sesion");
    }

    public Tokens refrescar(String refreshToken) {
        Respuesta r = enviar("POST", "/auth/v1/token?grant_type=refresh_token",
                Map.of("refresh_token", refreshToken), null);
        if (r.estado == 400 || r.estado == 401 || r.estado == 422) throw AuthException.sesionExpirada();
        return aTokens(r, "No se pudo renovar la sesion");
    }

    /** Cierra solo esta sesion (scope=local); las demas cuentas o dispositivos siguen activos. */
    public void cerrarSesion(String accessToken) {
        Respuesta r = enviar("POST", "/auth/v1/logout?scope=local", null, accessToken);
        if (r.estado / 100 != 2 && r.estado != 401 && r.estado != 403 && r.estado != 404) {
            throw AuthException.supabaseNoDisponible("Supabase no pudo cerrar la sesion");
        }
    }

    private Tokens aTokens(Respuesta r, String errorGenerico) {
        if (r.estado / 100 != 2 || r.cuerpo == null) {
            throw AuthException.supabaseNoDisponible(errorGenerico + " (Supabase respondio " + r.estado + ")");
        }
        JsonNode user = r.cuerpo.path("user");
        JsonNode meta = user.path("user_metadata");
        return new Tokens(r.cuerpo.path("access_token").asText(""), r.cuerpo.path("refresh_token").asText(""),
                r.cuerpo.path("expires_in").asLong(0), user.path("id").asText(""), user.path("email").asText(""),
                meta.path("nombre").asText(""), meta.path("usuario").asText(""));
    }

    private record Respuesta(int estado, JsonNode cuerpo) {
        String texto(String campo) {
            return cuerpo == null ? "" : cuerpo.path(campo).asText("");
        }
    }

    private Respuesta enviar(String metodo, String ruta, Object cuerpo, String bearer) {
        if (!estaConfigurado()) throw AuthException.noConfigurado();
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + ruta))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json");
            if (bearer != null) {
                b.header("Authorization", "Bearer " + bearer);
            } else if (anonKey.startsWith("eyJ")) {
                // Clave "anon" clasica (es un JWT): puede ir tambien como Bearer.
                b.header("Authorization", "Bearer " + anonKey);
            }
            // Clave publicable moderna (sb_publishable_...): NO es un JWT, solo va en la cabecera apikey.
            b.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)));
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode nodo = null;
            try {
                nodo = resp.body() == null || resp.body().isBlank() ? null : json.readTree(resp.body());
            } catch (JacksonException e) {
                // respuesta que no es JSON
            }
            return new Respuesta(resp.statusCode(), nodo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AuthException.supabaseNoDisponible("Peticion a Supabase interrumpida");
        } catch (IOException | IllegalArgumentException e) {
            log.warn("No se pudo contactar con Supabase: {}", e.toString());
            throw AuthException.supabaseNoDisponible("No se pudo conectar con Supabase. Revisa la conexion a internet");
        }
    }
}
