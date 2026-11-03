package com.smartcar.backend.models;

public class SensorAprox extends Sensor {
    private float rangoDeteccion;
    private float distancia;

    public SensorAprox(int idSensor, float rangoDeteccion) {
        super(idSensor);
        this.rangoDeteccion = rangoDeteccion;
        this.distancia = rangoDeteccion;
    }

    public float getRangoDeteccion() {
        return rangoDeteccion;
    }

    public float getDistancia() {
        return distancia;
    }

    public void setDistancia(float distancia) {
        this.distancia = distancia;
    }

    public float medirDistancia() {
        return distancia;
    }

    @Override
    public float leerDatos() {
        return medirDistancia();
    }
}
