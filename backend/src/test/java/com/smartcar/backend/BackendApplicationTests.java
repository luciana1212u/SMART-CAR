package com.smartcar.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.comunicacion.Esp32BleTransport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "smartcar.ble.reconexion-inicial-ms=600000")
class BackendApplicationTests {

    @Autowired
    private IEsp32Transport transporte;

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

	@Test
	void registroLoginEstadoYLogout() throws Exception {
        assertInstanceOf(Esp32BleTransport.class, transporte);
        assertFalse(transporte.estaConectado());
        assertEquals(401, pedir("GET", "/api/estado", null).statusCode());
        assertEquals(400, pedir("POST", "/api/auth/registro", "{}").statusCode());
        assertEquals(201, pedir("POST", "/api/auth/registro",
                "{\"nombre\":\"Prueba\",\"usuario\":\"prueba\",\"password\":\"clave\"}").statusCode());
        assertEquals(401, pedir("POST", "/api/auth/login",
                "{\"usuario\":\"prueba\",\"password\":\"incorrecta\"}").statusCode());
        var login = pedir("POST", "/api/auth/login",
                "{\"usuario\":\"prueba\",\"password\":\"clave\"}");
        assertEquals(200, login.statusCode());
        assertFalse(login.body().contains("passwordHash"));
        var estado = pedir("GET", "/api/estado", null);
        assertEquals(200, estado.statusCode());
        assertTrue(estado.body().contains("esp32Conectada"));
        assertEquals(503, pedir("POST", "/api/movimiento", "{\"comando\":\"AVANZAR\"}").statusCode());
        assertEquals(400, pedir("POST", "/api/movimiento", "{\"comando\":\"INVALIDO\"}").statusCode());
        assertEquals(200, pedir("POST", "/api/auth/logout", "{}").statusCode());
        assertEquals(401, pedir("GET", "/api/estado", null).statusCode());
	}

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo)).build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

}
