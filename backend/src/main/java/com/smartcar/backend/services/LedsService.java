package com.smartcar.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcar.backend.models.Leds;

@Service
public class LedsService {

    private Leds leds;
    private SensorService sensorService;

    // Spring inyecta SensorService automáticamente
    @Autowired
    public LedsService(SensorService sensorService) {
        this(new Leds(), sensorService);
    }

    // Constructor para pruebas
    public LedsService(Leds leds, SensorService sensorService) {
        this.leds = leds;
        this.sensorService = sensorService;
    }

    // Regla: los LEDs se encienden si está oscuro O si hay un objeto muy cerca
    // (sensor de proximidad). Si no se cumple ninguna de las dos, se apagan.
    // Se evalúan juntas para que una condición no apague los LEDs de la otra.
    public void evaluarOscuridad() {
        boolean estaOscuro = sensorService.hayOscuridad();
        boolean objetoCerca = sensorService.hayObstaculo();

        if (estaOscuro || objetoCerca) {
            leds.encender();
        } else {
            leds.apagar();
        }
    }

    // Regla: el LED de la placa queda encendido cuando el carro está quieto
    public void actualizarLedPlaca(boolean estaQuieto) {
        if (estaQuieto) {
            leds.encender();
        } else {
            leds.apagar();
        }
    }

    public Leds getLeds() {
        return leds;
    }
}
