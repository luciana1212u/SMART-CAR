package com.smartcar.backend.services;

import java.util.Random;

public class EvasionService {
    private final MotorService motorService;
    private final SensorService sensorService;
    private int tiempoMaxGiroMs = 1500;
    private final Random random = new Random();

    public EvasionService(MotorService motorService, SensorService sensorService) {
        this.motorService = motorService;
        this.sensorService = sensorService;
    }

    // Detiene, elige un lado y gira para esquivar el obstaculo
    public void evadir() {
        motorService.detenerMotores();
        if ("IZQUIERDA".equals(elegirLado())) {
            motorService.girarIzquierda();
        } else {
            motorService.girarDerecha();
        }
    }

    public String elegirLado() {
        return random.nextBoolean() ? "IZQUIERDA" : "DERECHA";
    }

    public int getTiempoMaxGiroMs() {
        return tiempoMaxGiroMs;
    }

    public void setTiempoMaxGiroMs(int tiempoMaxGiroMs) {
        this.tiempoMaxGiroMs = tiempoMaxGiroMs;
    }
}
