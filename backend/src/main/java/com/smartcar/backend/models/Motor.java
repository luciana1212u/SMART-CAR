package com.smartcar.backend.models;

public class Motor {

    private boolean estaEncendido;
    private String sentidoGiro;

    public Motor() {
        this.estaEncendido = false;
        this.sentidoGiro = "Detenido";
    }

    public Motor(boolean estaEncendido, String sentidoGiro) {
        this.estaEncendido = estaEncendido;
        this.sentidoGiro = sentidoGiro;
    }

    public void girarAdelante() {
        this.estaEncendido = true;
        this.sentidoGiro = "Adelante";
    }

    public void girarAtras() {
        this.estaEncendido = true;
        this.sentidoGiro = "Atrás";
    }

    public void detener() {
        this.estaEncendido = false;
        this.sentidoGiro = "Detenido";
    }

    public boolean getEstaEncendido() {
        return estaEncendido;
    }

    public void setEstaEncendido(boolean estaEncendido) {
        this.estaEncendido = estaEncendido;
        if (!estaEncendido) {
            this.sentidoGiro = "Detenido";
        }
    }

    public String getSentidoGiro() {
        return sentidoGiro;
    }

    public void setSentidoGiro(String sentidoGiro) {
        this.sentidoGiro = sentidoGiro;
    }
}