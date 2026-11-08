package com.smartcar.backend;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.config.AppConfig;
import com.smartcar.backend.excepciones.BateriaBajaException;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import com.smartcar.backend.models.Carro;
import com.smartcar.backend.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MicrocontroladorServiceTests {
    private Carro carro;
    private IAuthService auth;
    private IEsp32Transport transporte;
    private MicrocontroladorService servicio;

    @BeforeEach
    void preparar() {
        AppConfig config = new AppConfig();
        carro = config.carro();
        auth = mock(IAuthService.class);
        transporte = mock(IEsp32Transport.class);
        SensorService sensores = config.sensorService(carro);
        MotorService motores = config.motorService(carro);
        servicio = config.microcontroladorService(carro, sensores,
                config.ledsService(carro, sensores, auth, transporte), motores,
                config.bocinaService(carro, auth, transporte), auth,
                config.bateriaService(carro, 20), config.evasionService(motores, sensores), transporte);
    }

    @Test
    void requiereSesionYValidaComandosAntesDeEnviar() {
        assertThrows(SesionNoIniciadaException.class, () -> servicio.recibirComando("AVANZAR"));
        when(auth.estaAutenticado()).thenReturn(true);
        for (String comando : new String[]{"", "DESCONOCIDO", "VEL:79", "DIST:101", "VEL:abc"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.recibirComando(comando));
        }
        verify(transporte, never()).enviar(anyString());
        servicio.recibirComando(" avanzar ");
        verify(transporte).enviar("AVANZAR");
    }

    @Test
    void bateriaBajaBloqueaMovimientoPeroPermiteDetener() {
        when(auth.estaAutenticado()).thenReturn(true);
        servicio.onMensaje("D:120,L:70,B:10");
        assertThrows(BateriaBajaException.class, () -> servicio.recibirComando("MODO_AUTO"));
        servicio.recibirComando("DETENER");
        verify(transporte).enviar("DETENER");
        verify(transporte, never()).enviar("MODO_AUTO");
    }

    @Test
    void integraLosTresPaquetesDeTelemetriaYLaDesconexion() {
        when(transporte.estaConectado()).thenReturn(true);
        servicio.onMensaje("D:15,L:20,B:85");
        servicio.onMensaje("M:A,F:1,H:2,X:2");
        servicio.onMensaje("P:78,S:23,U:8.0");
        servicio.onMensaje("EV:OBSTACULO");
        var estado = servicio.consultarEstadoCompleto();
        assertTrue(estado.telemetriaActiva());
        assertEquals("AUTO", estado.modo());
        assertEquals("RETROCEDIENDO", estado.estado());
        assertEquals(85, estado.bateriaPct());
        assertEquals(23f, estado.velocidadCms());
        assertTrue(estado.hayObstaculo());
        assertTrue(estado.hayOscuridad());
        assertTrue(estado.lucesEncendidas());
        assertTrue(estado.bocinaSonando());
        assertEquals("EV:OBSTACULO", estado.ultimoEvento());
        servicio.onMensaje("D:NaN,L:20,B:85");
        assertEquals(15f, servicio.consultarEstadoCompleto().distanciaCm());
        servicio.onDesconectado();
        estado = servicio.consultarEstadoCompleto();
        assertFalse(estado.telemetriaActiva());
        assertEquals("QUIETO", estado.estado());
        assertEquals(0f, estado.velocidadCms());
        assertFalse(estado.bocinaSonando());
    }

    @Test
    void luzNoSeConfundeConBateriaAusenteNiBloqueaElMovimiento() {
        when(auth.estaAutenticado()).thenReturn(true);
        servicio.onMensaje("D:120,L:100,B:-1");
        servicio.onMensaje("P:78,S:-1,U:-1.0");
        assertEquals(-1, servicio.consultarBateria());
        assertFalse(servicio.consultarEstadoCompleto().bateriaBaja());
        servicio.recibirComando("AVANZAR");
        verify(transporte).enviar("AVANZAR");
        servicio.onMensaje("D:120,L:0,B:-1");
        assertTrue(servicio.consultarEstadoCompleto().hayOscuridad());
        assertFalse(servicio.consultarEstadoCompleto().bateriaBaja());
        servicio.recibirComando("RETROCEDER");
        verify(transporte).enviar("RETROCEDER");
    }

    @Test
    void supervisorNoEscribeSinConexionYDetieneTrasCerrarSesion() {
        servicio.ejecutarCiclo();
        verify(transporte, never()).enviar(anyString());
        when(transporte.estaConectado()).thenReturn(true);
        servicio.onMensaje("M:M,F:0,H:0,X:1");
        servicio.ejecutarCiclo();
        verify(transporte).enviar("DETENER");
        assertEquals("QUIETO", carro.getEstado());
    }
}
