package com.smartcar.backend;

import com.smartcar.backend.comunicacion.Esp32BleTransport;
import com.smartcar.backend.comunicacion.IEsp32Transport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Flujo completo contra un Supabase simulado: registro, login, JWT, refresh, logout y endpoints del carro. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "smartcar.ble.reconexion-inicial-ms=600000")
class BackendApplicationTests {
    private static final FakeSupabase SUPABASE;

    static {
        try {
            SUPABASE = new FakeSupabase();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void supabase(DynamicPropertyRegistry registro) {
        registro.add("smartcar.supabase.url", SUPABASE::url);
        registro.add("smartcar.supabase.anon-key", () -> "clave-publicable-de-prueba");
        registro.add("smartcar.supabase.jwt-secret", () -> FakeSupabase.SECRETO);
    }

    @AfterAll
    static void cerrar() {
        SUPABASE.close();
    }

    @Autowired
    private IEsp32Transport transporte;

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void registroLoginJwtRefreshYLogout() throws Exception {
        assertInstanceOf(Esp32BleTransport.class, transporte);
        assertFalse(transporte.estaConectado());

        // Sin JWT o con JWT invalido no se entra a ningun endpoint protegido (incluidos los del carro)
        assertEquals(401, pedir("GET", "/api/estado", null, null).statusCode());
        assertEquals(401, pedir("GET", "/api/estado", null, "token-falso").statusCode());
        assertEquals(401, pedir("POST", "/api/movimiento", "{\"comando\":\"AVANZAR\"}", null).statusCode());
        assertEquals(401, pedir("PUT", "/api/accesorios/bocina", "{\"activar\":true}", null).statusCode());
        assertEquals(401, pedir("GET", "/api/auth/sesion", null, null).statusCode());

        // Registro: valida el formato antes de llamar a Supabase
        assertEquals(400, pedir("POST", "/api/auth/registro", "{}", null).statusCode());
        assertEquals(400, pedir("POST", "/api/auth/registro",
                reg("Ana", "Ana Maria!", "ana@correo.com", "clave12345"), null).statusCode());
        assertEquals(400, pedir("POST", "/api/auth/registro",
                reg("Ana", "ana", "no-es-correo", "clave12345"), null).statusCode());
        assertEquals(400, pedir("POST", "/api/auth/registro",
                reg("Ana", "ana", "ana@correo.com", "corta"), null).statusCode());

        var creada = pedir("POST", "/api/auth/registro", reg("Ana Perez", "Ana_01", "Ana@Correo.com", "clave12345"), null);
        assertEquals(201, creada.statusCode());
        assertFalse(creada.body().contains("password"));
        // nombre y usuario viajan en data (lo que lee el trigger); el usuario se normaliza a minusculas
        JsonNode signup = SUPABASE.ultimoSignup;
        assertEquals("Ana Perez", signup.path("data").path("nombre").asText());
        assertEquals("ana_01", signup.path("data").path("usuario").asText());
        assertEquals("ana@correo.com", signup.path("email").asText());

        // Duplicados: usuario y correo se distinguen, y un fallo del trigger por otra causa NO es "duplicado"
        var usuarioRepetido = pedir("POST", "/api/auth/registro", reg("Otra", "ana_01", "otra@correo.com", "clave12345"), null);
        assertEquals(409, usuarioRepetido.statusCode());
        assertEquals("USUARIO_DUPLICADO", codigo(usuarioRepetido));
        var correoRepetido = pedir("POST", "/api/auth/registro", reg("Otra", "otra_01", "ana@correo.com", "clave12345"), null);
        assertEquals(409, correoRepetido.statusCode());
        assertEquals("CORREO_DUPLICADO", codigo(correoRepetido));
        assertTrue(pedir("GET", "/api/auth/usuario-disponible?usuario=ana_01", null, null)
                .body().contains("\"disponible\":false"));
        assertTrue(pedir("GET", "/api/auth/usuario-disponible?usuario=libre_99", null, null)
                .body().contains("\"disponible\":true"));

        SUPABASE.fallarTrigger = true;
        var fallo = pedir("POST", "/api/auth/registro", reg("Luis", "luis_01", "luis@correo.com", "clave12345"), null);
        assertEquals(502, fallo.statusCode());
        assertEquals("REGISTRO_FALLIDO", codigo(fallo));
        SUPABASE.rpcDisponible = false;      // sin la funcion SQL tampoco se puede afirmar que sea duplicado
        var sinRpc = pedir("POST", "/api/auth/registro", reg("Luis", "luis_01", "luis@correo.com", "clave12345"), null);
        assertEquals(502, sinRpc.statusCode());
        SUPABASE.fallarTrigger = false;
        SUPABASE.rpcDisponible = true;

        // Login
        assertEquals(401, pedir("POST", "/api/auth/login", "{\"email\":\"ana@correo.com\",\"password\":\"mala\"}", null).statusCode());
        assertEquals("CREDENCIALES", codigo(pedir("POST", "/api/auth/login",
                "{\"email\":\"nadie@correo.com\",\"password\":\"clave12345\"}", null)));
        var login = pedir("POST", "/api/auth/login", "{\"email\":\"ANA@correo.com\",\"password\":\"clave12345\"}", null);
        assertEquals(200, login.statusCode());
        JsonNode sesionA = json.readTree(login.body());
        String accessA = sesionA.path("accessToken").asText();
        String refreshA = sesionA.path("refreshToken").asText();
        assertEquals("ana_01", sesionA.path("usuario").path("usuario").asText());
        assertEquals("Ana Perez", sesionA.path("usuario").path("nombre").asText());

        // Con JWT valido se entra; el comando sigue pasando por las reglas del carro (ESP32 sin conectar -> 503)
        var sesion = pedir("GET", "/api/auth/sesion", null, accessA);
        assertEquals(200, sesion.statusCode());
        assertTrue(sesion.body().contains("ana@correo.com"));
        var estado = pedir("GET", "/api/estado", null, accessA);
        assertEquals(200, estado.statusCode());
        assertTrue(estado.body().contains("esp32Conectada"));
        assertEquals(503, pedir("POST", "/api/movimiento", "{\"comando\":\"AVANZAR\"}", accessA).statusCode());
        assertEquals(400, pedir("POST", "/api/movimiento", "{\"comando\":\"INVALIDO\"}", accessA).statusCode());

        // Segunda sesion independiente de la misma cuenta (p. ej. web y movil)
        JsonNode sesionB = json.readTree(pedir("POST", "/api/auth/login",
                "{\"email\":\"ana@correo.com\",\"password\":\"clave12345\"}", null).body());
        String accessB = sesionB.path("accessToken").asText();
        assertNotEquals(accessA, accessB);
        assertNotEquals(refreshA, sesionB.path("refreshToken").asText());

        // Refresh
        var renovada = pedir("POST", "/api/auth/refresh", "{\"refreshToken\":\"" + refreshA + "\"}", null);
        assertEquals(200, renovada.statusCode());
        String refreshNuevo = json.readTree(renovada.body()).path("refreshToken").asText();
        assertEquals(401, pedir("POST", "/api/auth/refresh", "{\"refreshToken\":\"inventado\"}", null).statusCode());

        // Logout de A: no afecta a B
        assertEquals(200, pedir("POST", "/api/auth/logout", "{}", accessA).statusCode());
        assertEquals(401, pedir("POST", "/api/auth/refresh", "{\"refreshToken\":\"" + refreshNuevo + "\"}", null).statusCode());
        assertEquals(200, pedir("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + sesionB.path("refreshToken").asText() + "\"}", null).statusCode());
        assertEquals(200, pedir("GET", "/api/estado", null, accessB).statusCode());
        assertEquals(401, pedir("POST", "/api/auth/logout", "{}", null).statusCode());
    }

    @Test
    void rechazaJwtManipuladosOVencidos() throws Exception {
        byte[] secreto = FakeSupabase.SECRETO.getBytes();
        String emisor = SUPABASE.url() + "/auth/v1";
        var futuro = java.time.Instant.now().plusSeconds(600);
        String bueno = SUPABASE.emitir("u1", "a@a.com", "A", "aaa", "s1", futuro, emisor, "authenticated", secreto);
        assertEquals(200, pedir("GET", "/api/estado", null, bueno).statusCode());

        String vencido = SUPABASE.emitir("u1", "a@a.com", "A", "aaa", "s1",
                java.time.Instant.now().minusSeconds(600), emisor, "authenticated", secreto);
        var r = pedir("GET", "/api/estado", null, vencido);
        assertEquals(401, r.statusCode());
        assertEquals("SESION_EXPIRADA", codigo(r));

        byte[] otro = "otro-secreto-distinto-de-mas-de-32-caracteres!!".getBytes();
        assertEquals(401, pedir("GET", "/api/estado", null,
                SUPABASE.emitir("u1", "a@a.com", "A", "aaa", "s1", futuro, emisor, "authenticated", otro)).statusCode());
        assertEquals(401, pedir("GET", "/api/estado", null,
                SUPABASE.emitir("u1", "a@a.com", "A", "aaa", "s1", futuro, "https://otro.supabase.co/auth/v1",
                        "authenticated", secreto)).statusCode());
        assertEquals(401, pedir("GET", "/api/estado", null,
                SUPABASE.emitir("u1", "a@a.com", "A", "aaa", "s1", futuro, emisor, "anon", secreto)).statusCode());
        // Cuerpo alterado sin volver a firmar
        String[] partes = bueno.split("\\.");
        String alterado = partes[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin\",\"iss\":\"x\",\"aud\":\"authenticated\",\"exp\":9999999999}".getBytes()) + "." + partes[2];
        assertEquals(401, pedir("GET", "/api/estado", null, alterado).statusCode());
        // alg "none"
        String sinFirma = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
                + "." + partes[1] + ".";
        assertEquals(401, pedir("GET", "/api/estado", null, sinFirma).statusCode());
    }

    private String reg(String nombre, String usuario, String email, String password) {
        return String.format("{\"nombre\":\"%s\",\"usuario\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                nombre, usuario, email, password);
    }

    private String codigo(HttpResponse<String> r) throws Exception {
        return json.readTree(r.body()).path("error").asText();
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return cliente.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
