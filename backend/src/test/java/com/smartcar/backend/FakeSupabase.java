package com.smartcar.backend;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Supabase simulado (Auth + RPC) para probar el backend sin internet ni credenciales reales. */
public class FakeSupabase implements AutoCloseable {
    public static final String SECRETO = "secreto-de-prueba-con-mas-de-treinta-y-dos-caracteres";

    private record Cuenta(String id, String password, String nombre, String usuario) { }

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpServer servidor;
    private final String url;
    private final Map<String, Cuenta> cuentas = new ConcurrentHashMap<>();           // email -> cuenta
    private final Map<String, String> refreshActivos = new ConcurrentHashMap<>();    // refresh -> sessionId
    private final Set<String> usuarios = new HashSet<>();
    public volatile boolean rpcDisponible = true;
    public volatile boolean fallarTrigger = false;   // el trigger falla por otra causa distinta del usuario duplicado
    public volatile JsonNode ultimoSignup;
    public volatile long tokenVidaSeg = 3600;

    public FakeSupabase() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        url = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public String url() {
        return url;
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    // ---- fabricacion de tokens (tambien para tokens manipulados en las pruebas) ----

    public String emitir(String sub, String email, String nombre, String usuario, String sessionId,
                         Instant expira, String emisor, String audiencia, byte[] secreto) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(sub).issuer(emisor).audience(audiencia)
                    .expirationTime(Date.from(expira)).issueTime(new Date()).claim("email", email)
                    .claim("session_id", sessionId).claim("role", "authenticated")
                    .claim("user_metadata", Map.of("nombre", nombre, "usuario", usuario)).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims);
            JWSSigner firmante = new MACSigner(secreto);
            jwt.sign(firmante);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String accessToken(Cuenta c, String sessionId) {
        return emitir(c.id(), emailDe(c), c.nombre(), c.usuario(), sessionId,
                Instant.now().plusSeconds(tokenVidaSeg), url + "/auth/v1", "authenticated",
                SECRETO.getBytes(StandardCharsets.UTF_8));
    }

    private String emailDe(Cuenta c) {
        return cuentas.entrySet().stream().filter(e -> e.getValue() == c).map(Map.Entry::getKey).findFirst().orElse("");
    }

    // ---- servidor ----

    private void atender(HttpExchange ex) {
        try {
            String ruta = ex.getRequestURI().getPath();
            String consulta = ex.getRequestURI().getQuery() == null ? "" : ex.getRequestURI().getQuery();
            String texto = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode cuerpo = texto.isBlank() ? json.createObjectNode() : json.readTree(texto);
            String apikey = ex.getRequestHeaders().getFirst("apikey");
            if (apikey == null || apikey.isBlank()) {
                responder(ex, 401, Map.of("msg", "No API key found in request"));
                return;
            }
            // Las claves sb_publishable_ no son JWT: enviarlas como Bearer hace que la pasarela las rechace
            String autorizacion = ex.getRequestHeaders().getFirst("Authorization");
            if (apikey.startsWith("sb_") && autorizacion != null && autorizacion.equals("Bearer " + apikey)) {
                responder(ex, 401, Map.of("msg", "Invalid JWT"));
                return;
            }
            switch (ruta) {
                case "/auth/v1/signup" -> signup(ex, cuerpo);
                case "/auth/v1/token" -> token(ex, consulta, cuerpo);
                case "/auth/v1/logout" -> logout(ex);
                case "/rest/v1/rpc/usuario_disponible" -> rpc(ex, cuerpo);
                default -> responder(ex, 404, Map.of("msg", "no existe " + ruta));
            }
        } catch (Exception e) {
            try { responder(ex, 500, Map.of("msg", String.valueOf(e))); } catch (Exception ignorada) { /* nada */ }
        }
    }

    private synchronized void signup(HttpExchange ex, JsonNode cuerpo) throws Exception {
        ultimoSignup = cuerpo;
        String email = cuerpo.path("email").asText("");
        String usuario = cuerpo.path("data").path("usuario").asText("");
        if (cuerpo.path("password").asText("").length() < 6) {
            responder(ex, 422, Map.of("error_code", "weak_password", "msg", "Password should be at least 6 characters."));
        } else if (cuentas.containsKey(email)) {
            responder(ex, 422, Map.of("error_code", "user_already_exists", "msg", "User already registered"));
        } else if (fallarTrigger || usuarios.contains(usuario)) {
            // Asi falla Supabase cuando el trigger viola el unique de perfiles.usuario
            responder(ex, 500, Map.of("code", 500, "error_code", "unexpected_failure",
                    "msg", "Database error saving new user"));
        } else {
            Cuenta c = new Cuenta(UUID.randomUUID().toString(), cuerpo.path("password").asText(),
                    cuerpo.path("data").path("nombre").asText(""), usuario);
            cuentas.put(email, c);
            usuarios.add(usuario);
            responder(ex, 200, Map.of("id", c.id(), "email", email));
        }
    }

    private void token(HttpExchange ex, String consulta, JsonNode cuerpo) throws Exception {
        if (consulta.contains("grant_type=password")) {
            Cuenta c = cuentas.get(cuerpo.path("email").asText(""));
            if (c == null || !c.password().equals(cuerpo.path("password").asText())) {
                responder(ex, 400, Map.of("error_code", "invalid_credentials", "msg", "Invalid login credentials"));
                return;
            }
            responderSesion(ex, c, UUID.randomUUID().toString());
        } else if (consulta.contains("grant_type=refresh_token")) {
            String refresh = cuerpo.path("refresh_token").asText("");
            String sesion = refreshActivos.remove(refresh);
            if (sesion == null) {
                responder(ex, 400, Map.of("error_code", "refresh_token_not_found", "msg", "Invalid Refresh Token"));
                return;
            }
            Cuenta c = cuentas.values().stream().filter(x -> refresh.startsWith(x.id())).findFirst().orElseThrow();
            responderSesion(ex, c, sesion);
        } else {
            responder(ex, 400, Map.of("msg", "grant_type no soportado"));
        }
    }

    private void responderSesion(HttpExchange ex, Cuenta c, String sesion) throws Exception {
        String refresh = c.id() + "." + UUID.randomUUID();
        refreshActivos.put(refresh, sesion);
        responder(ex, 200, Map.of("access_token", accessToken(c, sesion), "refresh_token", refresh,
                "expires_in", tokenVidaSeg, "token_type", "bearer",
                "user", Map.of("id", c.id(), "email", emailDe(c),
                        "user_metadata", Map.of("nombre", c.nombre(), "usuario", c.usuario()))));
    }

    private void logout(HttpExchange ex) throws Exception {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        try {
            String sesion = SignedJWT.parse(auth.substring(7)).getJWTClaimsSet().getStringClaim("session_id");
            refreshActivos.values().removeIf(sesion::equals);   // solo cierra ESTA sesion
        } catch (Exception e) {
            responder(ex, 401, Map.of("msg", "invalid token"));
            return;
        }
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private synchronized void rpc(HttpExchange ex, JsonNode cuerpo) throws Exception {
        if (!rpcDisponible) {
            responder(ex, 404, Map.of("code", "PGRST202", "message", "Could not find the function"));
            return;
        }
        responder(ex, 200, !usuarios.contains(cuerpo.path("p_usuario").asText("")));
    }

    private void responder(HttpExchange ex, int estado, Object cuerpo) throws Exception {
        byte[] bytes = json.writeValueAsString(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(estado, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public List<String> correos() {
        return List.copyOf(cuentas.keySet());
    }
}
