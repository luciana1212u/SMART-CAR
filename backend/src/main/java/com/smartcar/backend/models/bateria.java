package com.smartcar.backend.models;

public class Bateria {

    private int nivel;
    private float voltaje;

    public Bateria() {
    }

    public Bateria(int nivel, float voltaje) {
        this.nivel = nivel;
        this.voltaje = voltaje;
    }

    public int ObtenerNivel() {
        return this.nivel;
    }

    public boolean estaBaja() {
        return this.nivel <= 20;
    }

    public void recargar() {
        this.nivel = 100;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public float getVoltaje() {
        return voltaje;
    }

    public void setVoltaje(float voltaje) {
        this.voltaje = voltaje;
    }
}
