package com.smartcar.backend.config;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.services.MicrocontroladorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dos tareas de fondo: mantener el enlace Bluetooth y supervisar el carro. */
@Component
public class SupervisorTask {
    private final IEsp32Transport transporte;
    private final MicrocontroladorService microcontroladorService;

    public SupervisorTask(IEsp32Transport transporte, MicrocontroladorService microcontroladorService) {
        this.transporte = transporte;
        this.microcontroladorService = microcontroladorService;
    }

    /** Si el enlace esta caido, intenta reconectar cada 5 s (el escaneo BLE tarda unos segundos). */
    @Scheduled(initialDelayString = "${smartcar.ble.reconexion-inicial-ms:2000}", fixedDelay = 5000)
    public void mantenerEnlace() {
        if (!transporte.estaConectado()) {
            transporte.conectar();
        }
    }

    @Scheduled(fixedDelay = 250)
    public void supervisar() {
        microcontroladorService.ejecutarCiclo();
    }
}
