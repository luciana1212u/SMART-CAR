package com.smartcar.backend;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.config.AppConfig;
import com.smartcar.backend.dto.SesionResponse;
import com.smartcar.backend.excepciones.AuthException;
import com.smartcar.backend.models.Carro;
import com.smartcar.backend.services.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSeguridadTests {
    /** Reloj que avanza solo cuando la prueba lo pide. */
    static class RelojManual extends Clock {
        private final AtomicLong ms = new AtomicLong(1_000_000);
        void avanzar(long milis) { ms.addAndGet(milis); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(ms.get()); }
        @Override public long millis() { return ms.get(); }
    }

    private FakeSupabase supabase;

    @BeforeEach
    void iniciar() throws Exception {
        supabase = new FakeSupabase();
    }

    @AfterEach
    void cerrar() {
        supabase.close();
    }

    private AuthService nuevoServicio(Clock reloj) {
        return new AuthService(new SupabaseAuthClient(supabase.url(), "clave-publicable"),
                new JwtValidator(supabase.url(), FakeSupabase.SECRETO), 3000, reloj);
    }

    @Test
    void lasCuentasSobrevivenAlReinicioDelBackend() {
        AuthService antes = nuevoServicio(Clock.systemUTC());
        antes.registrar("Ana", "ana_01", "ana@correo.com", "clave12345");

        // "Reinicio": una instancia nueva no guarda nada en memoria y aun asi la cuenta entra
        AuthService despues = nuevoServicio(Clock.systemUTC());
        SesionResponse sesion = despues.iniciarSesion("ana@correo.com", "clave12345");
        assertEquals("ana_01", sesion.usuario().usuario());
        assertEquals("ana@correo.com", despues.autenticarSolicitud(sesion.accessToken()).getEmail());
        assertThrows(AuthException.class, () -> despues.iniciarSesion("ana@correo.com", "otra-clave"));
    }

    @Test
    void funcionaConClavesPublicablesModernasYConLaAnonClasica() {
        for (String clave : new String[]{"sb_publishable_clave_de_prueba", "eyJhbGciOiJIUzI1NiJ9.anon.firma"}) {
            SupabaseAuthClient cliente = new SupabaseAuthClient(supabase.url(), clave);
            String correo = "u" + Math.abs(clave.hashCode()) + "@correo.com";
            String usuario = "u" + Math.abs(clave.hashCode());
            assertEquals(Boolean.TRUE, cliente.usuarioDisponible(usuario));
            cliente.registrar("Ana", usuario, correo, "clave12345");
            assertEquals(Boolean.FALSE, cliente.usuarioDisponible(usuario));
            assertEquals(usuario, cliente.iniciarSesion(correo, "clave12345").usuario());
        }
    }

    @Test
    void sinSesionHttpNoHayUsuarioAutenticado() {
        AuthService auth = nuevoServicio(Clock.systemUTC());
        auth.registrar("Ana", "ana_01", "ana@correo.com", "clave12345");
        auth.autenticarSolicitud(auth.iniciarSesion("ana@correo.com", "clave12345").accessToken());
        // Autenticar una peticion no deja estado global: fuera de una peticion nadie esta autenticado
        assertFalse(auth.estaAutenticado());
        assertNull(auth.getUsuarioAutenticado());
    }

    @Test
    void usuarioDuplicadoSeDetectaAunqueFalleLaComprobacionPrevia() {
        SupabaseAuthClient cliente = new SupabaseAuthClient(supabase.url(), "clave-publicable");
        cliente.registrar("Ana", "ana_01", "ana@correo.com", "clave12345");
        // Simula la carrera: dos registros a la vez saltandose la comprobacion previa
        AuthException e = assertThrows(AuthException.class,
                () -> cliente.registrar("Otra", "ana_01", "otra@correo.com", "clave12345"));
        assertEquals("USUARIO_DUPLICADO", e.getCodigo());
        assertEquals("CORREO_DUPLICADO", assertThrows(AuthException.class,
                () -> cliente.registrar("Otra", "otra_01", "ana@correo.com", "clave12345")).getCodigo());
    }

    @Test
    void supabaseCaidoSeInformaComoTal() throws Exception {
        String urlMuerta = supabase.url();
        supabase.close();
        AuthException e = assertThrows(AuthException.class,
                () -> new SupabaseAuthClient(urlMuerta, "clave").iniciarSesion("a@a.com", "12345678"));
        assertEquals("SUPABASE_NO_DISPONIBLE", e.getCodigo());
        assertEquals("AUTH_NO_CONFIGURADA", assertThrows(AuthException.class,
                () -> new SupabaseAuthClient("", "").iniciarSesion("a@a.com", "12345678")).getCodigo());
        supabase = new FakeSupabase();   // para el @AfterEach
    }

    @Test
    void validaClavesAsimetricasES256ViaJwksYRechazaAlgoritmosCambiados() throws Exception {
        ECKey clave = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
        ECKey ajena = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
        HttpServer jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] cuerpo = new JWKSet(clave.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks.createContext("/auth/v1/.well-known/jwks.json", ex -> {
            ex.sendResponseHeaders(200, cuerpo.length);
            ex.getResponseBody().write(cuerpo);
            ex.close();
        });
        jwks.start();
        try {
            String base = "http://127.0.0.1:" + jwks.getAddress().getPort();
            JwtValidator validador = new JwtValidator(base, "");   // sin secreto: usa JWKS
            JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("u1").issuer(base + "/auth/v1")
                    .audience("authenticated").claim("email", "a@a.com")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();

            assertEquals("a@a.com", validador.validar(firmarEs256(clave, claims)).getEmail());
            assertThrows(AuthException.class, () -> validador.validar(firmarEs256(ajena, claims)));
            // Confusion de algoritmo: un HS256 firmado con cualquier secreto no se acepta con claves asimetricas
            String hs = supabase.emitir("u1", "a@a.com", "A", "aaa", "s", Instant.now().plusSeconds(300),
                    base + "/auth/v1", "authenticated", FakeSupabase.SECRETO.getBytes(StandardCharsets.UTF_8));
            assertThrows(AuthException.class, () -> validador.validar(hs));
        } finally {
            jwks.stop(0);
        }
    }

    private static String firmarEs256(ECKey clave, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("k1").build(), claims);
        jwt.sign(new ECDSASigner(clave));
        return jwt.serialize();
    }

    // ---- Parada segura (simulada: transporte falso, ningun motor real) ----

    private MicrocontroladorService servicioConCarro(AuthService auth, IEsp32Transport transporte, Carro[] carroOut) {
        AppConfig config = new AppConfig();
        Carro carro = config.carro();
        carroOut[0] = carro;
        SensorService sensores = config.sensorService(carro);
        MotorService motores = config.motorService(carro);
        return config.microcontroladorService(carro, sensores,
                config.ledsService(carro, sensores, auth, transporte), motores,
                config.bocinaService(carro, auth, transporte), auth,
                config.bateriaService(carro, 20), config.evasionService(motores, sensores), transporte);
    }

    @Test
    void elCarroSeDetieneSiLaAppDejaDeComunicarse() {
        RelojManual reloj = new RelojManual();
        AuthService auth = nuevoServicio(reloj);
        auth.registrar("Ana", "ana_01", "ana@correo.com", "clave12345");
        String token = auth.iniciarSesion("ana@correo.com", "clave12345").accessToken();
        IEsp32Transport transporte = mock(IEsp32Transport.class);
        when(transporte.estaConectado()).thenReturn(true);
        Carro[] carro = new Carro[1];
        MicrocontroladorService servicio = servicioConCarro(auth, transporte, carro);

        servicio.onMensaje("M:M,F:0,H:0,X:1");          // la ESP32 reporta que avanza
        assertEquals("AVANZANDO", carro[0].getEstado());

        // Nunca hubo una peticion autenticada: no hay comunicacion vigente y el supervisor detiene el carro
        servicio.ejecutarCiclo();
        verify(transporte, times(1)).enviar("DETENER");

        // Con la app activa (latido) el carro sigue en marcha
        servicio.onMensaje("M:M,F:0,H:0,X:1");
        auth.autenticarSolicitud(token);
        servicio.ejecutarCiclo();
        reloj.avanzar(2900);
        servicio.ejecutarCiclo();
        verify(transporte, times(1)).enviar("DETENER");
        assertEquals("AVANZANDO", carro[0].getEstado());

        // Silencio mayor al limite: parada segura
        reloj.avanzar(200);
        servicio.ejecutarCiclo();
        verify(transporte, times(2)).enviar("DETENER");
        assertEquals("QUIETO", carro[0].getEstado());

        // Cerrar sesion tambien detiene el carro aunque el limite no haya vencido
        servicio.onMensaje("M:M,F:0,H:0,X:1");
        auth.autenticarSolicitud(token);
        auth.cerrarSesion(token);
        servicio.ejecutarCiclo();
        verify(transporte, times(3)).enviar("DETENER");
    }

    @Test
    void laComunicacionVencidaNoDependeDeUnaSesionHttp() {
        RelojManual reloj = new RelojManual();
        AuthService auth = nuevoServicio(reloj);
        assertFalse(auth.comunicacionVigente());
        auth.registrar("Ana", "ana_01", "ana@correo.com", "clave12345");
        auth.autenticarSolicitud(auth.iniciarSesion("ana@correo.com", "clave12345").accessToken());
        assertTrue(auth.comunicacionVigente());          // desde el hilo del supervisor (sin peticion HTTP)
        reloj.avanzar(Duration.ofSeconds(4).toMillis());
        assertFalse(auth.comunicacionVigente());
    }
}
