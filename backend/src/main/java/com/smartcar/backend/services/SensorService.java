package com.smartcar.backend.services;

import com.smartcar.backend.models.SensorAprox;
import com.smartcar.backend.models.SensorLuz;

public class SensorService {
    private final SensorAprox sensorAproximidad;
    private final SensorLuz sensorLuz;
    private float umbralDistancia = 20f;
    // L = 100 - milivoltios * 100 / 3300; 2.5 V equivale aproximadamente a 24%.
    private float umbralOscuridad = 24f;

    public SensorService(SensorAprox sensorAproximidad, SensorLuz sensorLuz) {
        this.sensorAproximidad = sensorAproximidad;
        this.sensorLuz = sensorLuz;
    }

    // Lo llama MicrocontroladorService con cada paquete D:<cm>,L:<luz%> de la ESP32
    public void actualizarLecturas(float distanciaCm, float luzPct) {
        sensorAproximidad.setDistancia(distanciaCm);
        sensorLuz.setIntensidad(luzPct);
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

    public float consultarDistancia() {
        return sensorAproximidad.medirDistancia();
    }

    public float consultarNivelLuz() {
        return sensorLuz.medirIntensidad();
    }

}
