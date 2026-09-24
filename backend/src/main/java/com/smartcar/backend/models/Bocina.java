package com.smartcar.backend.models;

public class Bocina {

    private boolean estaSonando;

    public void sonar() {
    }

    public void detener() {
    }

    public void evaluarActivacion(boolean hayObstaculo, boolean estaRetrocediendo) {
    }

    public boolean isEstaSonando() {
        return estaSonando;
    }

    public void setEstaSonando(boolean estaSonando) {
        this.estaSonando = estaSonando;
    }
}