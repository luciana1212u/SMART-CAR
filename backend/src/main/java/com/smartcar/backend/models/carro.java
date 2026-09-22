package com.smartcar.backend.models;

public class Carro {

    private String estado;
    private float velocidad;
    private String direccion;

    public Carro() {
    }

    public Carro(String estado, float velocidad, String direccion) {
        this.estado = estado;
        this.velocidad = velocidad;
        this.direccion = direccion;
    }

    public void avanzar() {
    }

    public void detener() {
    }

    public void cambiarDireccion(String nuevaDireccion) {
        this.direccion = nuevaDireccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
