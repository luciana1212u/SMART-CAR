package com.smartcar.backend.models;

public class Bateria {

    private int nivel;
    private float voltaje;

    public Bateria(int nivel, float voltaje) {
        this.nivel = nivel;
        this.voltaje = voltaje;
    }

    public int obtenerNivel() { return nivel; }

    public void actualizarNivel(int nivel) {
        this.nivel = Math.max(0, Math.min(100, nivel));
    }

    public float getVoltaje() {
        return voltaje;
    }

    public void setVoltaje(float voltaje) {
        this.voltaje = voltaje;
    }
}
