package com.smartcar.backend.services;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import com.smartcar.backend.models.Leds;

public class LedsService {
    private final Leds luces;        // luces delanteras del carro
    private final Leds ledPlaca;     // LED de la placa: encendido = carro quieto
    private final SensorService sensorService;
    private final IAuthService authService;
    private final IEsp32Transport transporte;
    private volatile String modoLuces = "AUTO";
    private Boolean ultimoLedPlacaEnviado = null;

    public LedsService(Leds luces, Leds ledPlaca, SensorService sensorService,
                       IAuthService authService, IEsp32Transport transporte) {
        this.luces = luces;
        this.ledPlaca = ledPlaca;
        this.sensorService = sensorService;
        this.authService = authService;
        this.transporte = transporte;
    }

    // ---- ordenes de la app (AccesoriosController) ----

    public void controlarLuces(boolean encender) {
        exigirSesion();
        transporte.enviar(encender ? "LUCES_ON" : "LUCES_OFF");
        modoLuces = encender ? "ON" : "OFF";
    }

    public void luzAutomatica() {
        exigirSesion();
        transporte.enviar("LUCES_AUTO");
        modoLuces = "AUTO";
    }

    // ---- logica de iluminacion ----

    // La luz ambiente controla las luces; un obstaculo no modifica este criterio.
    public boolean evaluarOscuridad() {
        return sensorService.hayOscuridad();
    }

    // El LED de la placa indica si el carro esta quieto. Solo se envia cuando cambia.
    public synchronized void actualizarLedPlaca(boolean estaQuieto) {
        ledPlaca.sincronizar(estaQuieto);
        if (ultimoLedPlacaEnviado != null && ultimoLedPlacaEnviado == estaQuieto) {
            return;
        }
        transporte.enviar(estaQuieto ? "ON" : "OFF");
        ultimoLedPlacaEnviado = estaQuieto;
    }

    // Tras una reconexion la ESP32 vuelve a su estado inicial: hay que reenviar
    public synchronized void reiniciarLedPlaca() {
        ultimoLedPlacaEnviado = null;
    }

    // Estado real de las luces segun la telemetria (campo F)
    public void sincronizarLuces(boolean encendidas) {
        luces.sincronizar(encendidas);
    }

    public boolean lucesEncendidas() {
        return luces.isEstado();
    }

    public String getModoLuces() {
        return modoLuces;
    }

    private void exigirSesion() {
        if (!authService.estaAutenticado()) {
            throw new SesionNoIniciadaException();
        }
    }
}
