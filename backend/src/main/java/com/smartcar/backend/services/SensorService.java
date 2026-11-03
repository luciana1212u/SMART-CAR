package com.smartcar.backend.services;

import com.smartcar.backend.models.SensorAprox;
import com.smartcar.backend.models.SensorLuz;

public class SensorService {
    private final SensorAprox sensorAproximidad;
    private final SensorLuz sensorLuz;
    private float umbralDistancia = 20f;
    private float umbralOscuridad = 30f;

    public SensorService(SensorAprox sensorAproximidad, SensorLuz sensorLuz) {
        this.sensorAproximidad = sensorAproximidad;
        this.sensorLuz = sensorLuz;
    }

    // Un sensor desactivado no reporta obstaculo
    public boolean hayObstaculo() {
        if (!sensorAproximidad.estaActivo()) {
            return false;
        }
        return sensorAproximidad.medirDistancia() < umbralDistancia;
    }

    // Un sensor desactivado no reporta oscuridad
    public boolean hayOscuridad() {
        if (!sensorLuz.estaActivo()) {
            return false;
        }
        return sensorLuz.medirIntensidad() < umbralOscuridad;
    }

    public float getUmbralDistancia() {
        return umbralDistancia;
    }

    public void setUmbralDistancia(float umbralDistancia) {
        this.umbralDistancia = umbralDistancia;
    }

    public float getUmbralOscuridad() {
        return umbralOscuridad;
    }

    public void setUmbralOscuridad(float umbralOscuridad) {
        this.umbralOscuridad = umbralOscuridad;
    }
}
