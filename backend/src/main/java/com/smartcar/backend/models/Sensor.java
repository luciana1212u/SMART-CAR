package com.smartcar.backend.models;

public abstract class Sensor implements ISensor {
    protected int idSensor;
    protected boolean estado;

    public Sensor(int idSensor) {
        this.idSensor = idSensor;
        this.estado = true;
    }

    public int getIdSensor() {
        return idSensor;
    }

    @Override
    public boolean estaActivo() {
        return estado;
    }

    @Override
    public void activar() {
        this.estado = true;
    }

    @Override
    public void desactivar() {
        this.estado = false;
    }
}
