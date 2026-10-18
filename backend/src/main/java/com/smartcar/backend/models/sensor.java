package com.smartcar.backend.models;

public class sensor {

    private int idSensor;
    private boolean estado;

    public sensor() {
    }

    public sensor(int idSensor, boolean estado) {
        this.idSensor = idSensor;
        this.estado = estado;
    }

    public float leerDatos() {
        return 0.0f;
    }

    public void activar() {
        this.estado = true;
    }

    public void desactivar() {
        this.estado = false;
    }

    public int getIdSensor() {
        return idSensor;
    }

    public void setIdSensor(int idSensor) {
        this.idSensor = idSensor;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}