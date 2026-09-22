package com.smartcar.backend.models;

public class Motor {

    private boolean estaEncendido;
    private String sentidoGiro;

    // Constructor vacío
    public Motor() {
    }

    // Constructor con parámetros
    public Motor(boolean estaEncendido, String sentidoGiro) {
        this.estaEncendido = estaEncendido;
        this.sentidoGiro = sentidoGiro;
    }

    // Comportamientos
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

    // Getters y Setters
    public boolean isEstaEncendido() {
        return estaEncendido;
    }

    public void setEstaEncendido(boolean estaEncendido) {
        this.estaEncendido = estaEncendido;
    }

    public String getSentidoGiro() {
        return sentidoGiro;
    }

    public void setSentidoGiro(String sentidoGiro) {
        this.sentidoGiro = sentidoGiro;
    }
}