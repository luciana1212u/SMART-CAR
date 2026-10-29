package com.smartcar.backend.models;

public class SensorLuz extends sensor {

    private float nivelLuz;

    public SensorLuz() {
        super();
    }

    public SensorLuz(
            int idSensor,
            boolean estado,
            float nivelLuz
    ) {
        super(idSensor, estado);
        this.nivelLuz = nivelLuz;
    }

    public float medirLuz() {
        return this.nivelLuz;
    }

    public boolean hayOscuridad() {
        return this.nivelLuz < 20.0f;
    }

    @Override
    public float leerDatos() {
        return this.nivelLuz;
    }

    public float getNivelLuz() {
        return nivelLuz;
    }

    public void setNivelLuz(float nivelLuz) {
        this.nivelLuz = nivelLuz;
    }
}