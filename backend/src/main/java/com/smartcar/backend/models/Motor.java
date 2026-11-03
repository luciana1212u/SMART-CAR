package com.smartcar.backend.models;

public class Motor {
    private int idMotor;
    private boolean estaEncendido;
    private String sentidoGiro;

    public Motor(int idMotor) {
        this.idMotor = idMotor;
        this.estaEncendido = false;
        this.sentidoGiro = "NINGUNO";
    }

    public int getIdMotor() {
        return idMotor;
    }

    public boolean isEstaEncendido() {
        return estaEncendido;
    }

    public String getSentidoGiro() {
        return sentidoGiro;
    }

    public void girarAdelante() {
        this.estaEncendido = true;
        this.sentidoGiro = "ADELANTE";
    }

    public void girarAtras() {
        this.estaEncendido = true;
        this.sentidoGiro = "ATRAS";
    }

    public void detener() {
        this.estaEncendido = false;
        this.sentidoGiro = "NINGUNO";
    }
}
