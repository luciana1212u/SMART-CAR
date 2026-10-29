package com.smartcar.backend.services;

import org.springframework.stereotype.Service;

import com.smartcar.backend.models.SensorAprox;
import com.smartcar.backend.models.SensorLuz;

@Service
public class SensorService {

    private SensorAprox sensorAproximidad;
    private SensorLuz sensorLuz;
    private float umbralDistancia;   // cm: más cerca que esto = obstáculo
    private float umbralOscuridad;   // nivel de luz: menos que esto = oscuro

    // Constructor que usa Spring: crea los sensores con valores por defecto
    public SensorService() {
        this(
            new SensorAprox(1, true, false, 30f, 100),
            new SensorLuz(2, true, 100f),
            20f,
            20f
        );
    }

    // Constructor para pruebas o para configurar otros sensores/umbrales
    public SensorService(SensorAprox sensorAproximidad, SensorLuz sensorLuz,
                         float umbralDistancia, float umbralOscuridad) {
        this.sensorAproximidad = sensorAproximidad;
        this.sensorLuz = sensorLuz;
        this.umbralDistancia = umbralDistancia;
        this.umbralOscuridad = umbralOscuridad;
    }

    // Regla: hay obstáculo si el sensor está activo y el objeto está más cerca que el umbral
    public boolean hayObstaculo() {
        if (!sensorAproximidad.isEstado()) {
            return false;
        }
        return sensorAproximidad.getDistancia() < umbralDistancia;
    }

    // Regla: está oscuro si el sensor está activo y el nivel de luz es menor que el umbral
    public boolean hayOscuridad() {
        if (!sensorLuz.isEstado()) {
            return false;
        }
        return sensorLuz.medirLuz() < umbralOscuridad;
    }

    public SensorAprox getSensorAproximidad() {
        return sensorAproximidad;
    }

    public SensorLuz getSensorLuz() {
        return sensorLuz;
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
