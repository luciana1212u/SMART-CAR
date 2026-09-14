package com.smartcar.backend.models;

public class carro {

    private String estado;
    private float velocidad;
    private String direccion;

    public void Carro() {
    }

    public void Carro(String estado, float velocidad, String direccion) {
        this.estado = estado;
        this.velocidad = velocidad;
        this.direccion = direccion;
    }

    public void avanzar() {

    }

    public void detener() {

    }

    public void cambiarDireccion() {

    }

    public String getEstado() {
        return estado;
    }

    public String setEstado() {
        return this.estado = estado;

    }

    public float getVelocidad() {
        return velocidad;
    }

    public float setVelocidad() {
        return this.velocidad = velocidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public String setDireccion() {
        return this.direccion = direccion;

    }
}
