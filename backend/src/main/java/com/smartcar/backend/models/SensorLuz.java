package com.smartcar.backend.models;

public class SensorLuz extends Sensor {
    private float intensidad;

    public SensorLuz(int idSensor) {
        super(idSensor);
        this.intensidad = 100f;
    }

    public float getIntensidad() {
        return intensidad;
    }

    public void setIntensidad(float intensidad) {
        this.intensidad = intensidad;
    }

    public float medirIntensidad() {
        return intensidad;
    }

    @Override
    public float leerDatos() {
        return medirIntensidad();
    }
}
