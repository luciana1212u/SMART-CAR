package com.smartcar.backend.models;

public class sensor_luz {

    private float nivelLuz;

    public sensor_luz() {
    }

    public sensor_luz(float nivelLuz) {
        this.nivelLuz = nivelLuz;
    }

    public float medirLuz() {
        return this.nivelLuz;
    }

    public boolean hayOscuridad() {
        return this.nivelLuz < 30;
    }

    public void calibrar(float nuevoNivelLuz) {
        this.nivelLuz = nuevoNivelLuz;
    }

    public float getNivelLuz() {
        return nivelLuz;
    }

    public void setNivelLuz(float nivelLuz) {
        this.nivelLuz = nivelLuz;
    }
}
