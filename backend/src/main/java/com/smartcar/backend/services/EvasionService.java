package com.smartcar.backend.services;

import org.springframework.stereotype.Service;

@Service
public class EvasionService {
    private final MotorService motorService;
    private final SensorService sensorService;
    private String ladoActual;          // se mantiene hasta que el obstaculo desaparece
    private boolean ultimoIzquierda;

    public EvasionService(MotorService motorService, SensorService sensorService) { 
        this.motorService = motorService;
        this.sensorService = sensorService;
    }

    public void evadir() {
        if (ladoActual == null) ladoActual = elegirLado();
        if ("IZQUIERDA".equals(ladoActual)) motorService.girarIzquierda();
        else motorService.girarDerecha();
    }

    public void liberar() { ladoActual = null; }

    public String elegirLado() {
        ultimoIzquierda = !ultimoIzquierda;
        return ultimoIzquierda ? "IZQUIERDA" : "DERECHA";
    }
}
