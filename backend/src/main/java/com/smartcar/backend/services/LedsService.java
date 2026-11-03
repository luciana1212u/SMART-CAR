package com.smartcar.backend.services;

import com.smartcar.backend.models.Leds;

public class LedsService {
    private final Leds leds;
    private final SensorService sensorService;

    public LedsService(Leds leds, SensorService sensorService) {
        this.leds = leds;
        this.sensorService = sensorService;
    }

    // Enciende los LEDs si esta oscuro o hay un objeto cerca; si no, los apaga
    public void evaluarOscuridad() {
        if (sensorService.hayOscuridad() || sensorService.hayObstaculo()) {
            leds.encender();
        } else {
            leds.apagar();
        }
    }

    // El LED de la placa indica si el carro esta quieto
    public void actualizarLedPlaca(boolean estaQuieto) {
        if (estaQuieto) {
            leds.encender();
        } else {
            leds.apagar();
        }
    }
}
