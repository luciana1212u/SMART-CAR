package com.smartcar.backend.models;

public class Leds {

    private boolean estado;

    public Leds() {
        this.estado = false;
    }

    public boolean isEstado() {
        return estado;
    }

    public void encender() {
        this.estado = true;
    }

    public void apagar() {
        this.estado = false;
    }

    public void sincronizar(boolean encendido) {
        this.estado = encendido;
    }
}
