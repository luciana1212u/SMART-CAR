package com.smartcar.backend.models;

public class SensorAprox extends Sensor {

    private boolean objetoDetectado;
    private float rangoDeteccion;
    private int distancia;

    public SensorAprox() {
        super();
    }

    public SensorAprox(
            int idSensor,
            boolean estado,
            boolean objetoDetectado,
            float rangoDeteccion,
            int distancia
    ) {
        super(idSensor, estado);
        this.objetoDetectado = objetoDetectado;
        this.rangoDeteccion = rangoDeteccion;
        this.distancia = distancia;
    }

    public boolean detectar() {
        this.objetoDetectado = this.distancia <= this.rangoDeteccion;
        return this.objetoDetectado;
    }

    public boolean hayObstaculo() {
        return this.objetoDetectado;
    }

    @Override
    public float leerDatos() {
        return this.distancia;
    }

    public boolean isObjetoDetectado() {
        return objetoDetectado;
    }

    public void setObjetoDetectado(boolean objetoDetectado) {
        this.objetoDetectado = objetoDetectado;
    }

    public float getRangoDeteccion() {
        return rangoDeteccion;
    }

    public void setRangoDeteccion(float rangoDeteccion) {
        this.rangoDeteccion = rangoDeteccion;
    }

    public int getDistancia() {
        return distancia;
    }

    public void setDistancia(int distancia) {
        this.distancia = distancia;
    }
}