package com.smartcar.backend.models;

import org.springframework.stereotype.Component;

@Component
public class carro {

    private String estado;
    private float velocidad;
    private String direccion;
    private String modoOperacion;

    public carro() {
        this.estado = "QUIETO";
        this.velocidad = 0f;
        this.direccion = "NINGUNA";
        this.modoOperacion = "REMOTO";
    }

    public carro(String estado, float velocidad, String direccion, String modoOperacion) {
        this.estado = estado;
        this.velocidad = velocidad;
        this.direccion = direccion;
        this.modoOperacion = modoOperacion;
    }

    public void cambiarEstado(String estado) {
        this.estado = estado;
    }

    public void cambiarDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean estaQuieto() {
        return "QUIETO".equals(this.estado);
    }

    public boolean estaRetrocediendo() {
        return "RETROCEDIENDO".equals(this.estado);
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

    public String getModoOperacion() {
        return modoOperacion;
    }

    public void setModoOperacion(String modoOperacion) {
        this.modoOperacion = modoOperacion;
    }
}