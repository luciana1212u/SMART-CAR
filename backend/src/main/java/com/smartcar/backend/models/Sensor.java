package com.smartcar.backend.models;

public abstract class Sensor implements ISensor {

    private int idSensor;
    private boolean estado;

    public sensor(int idSensor, boolean estado) {
        this.idSensor = idSensor;
        this.estado = estado;
    }

    @Override 
    public boolean estaActivo() { 
        return estado; }
    @Override 
    public void activar() { 
        estado = true; }
    @Override 
    public void desactivar() { 
        estado = false; }
    public int getIdSensor() { 
        return idSensor; }
}
