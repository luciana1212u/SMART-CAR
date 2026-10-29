package com.smartcar.backend.models;

public class Leds {

    private boolean estado;

    public Leds() {
        this.estado = false;
    }

    public Leds(boolean estado) {
        this.estado = estado;
    }

    public void encender() {
        this.estado = true;
    }

    public void apagar() {
        this.estado = false;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
