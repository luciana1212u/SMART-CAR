package com.smartcar.backend.models;

import org.springframework.stereotype.Component;

@Component
public class carro {

    private String estado;
    private float velocidad;
    private String direccion;
    private String modoOperacion;

    public carro() {
        this.estado = "Detenido";
        this.velocidad = 0;
        this.direccion = "Recto";
        this.modoOperacion = "REMOTO";
    }

    public carro(String estado, float velocidad, String direccion) {
        setEstado(estado);
        setVelocidad(velocidad);
        setDireccion(direccion);
        this.modoOperacion = "REMOTO";
    }

    public carro(String estado, float velocidad, String direccion, String modoOperacion) {
        setEstado(estado);
        setVelocidad(velocidad);
        setDireccion(direccion);
        this.modoOperacion = modoOperacion;
    }

    public void avanzar() {
        this.estado = "En movimiento";
    }

    public void detener() {
        this.velocidad = 0;
        this.estado = "Detenido";
    }

    public void cambiarEstado(String estado) {
        setEstado(estado);
    }

    public void cambiarDireccion(String nuevaDireccion) {
        setDireccion(nuevaDireccion);
    }

    public boolean estaQuieto() {
        return "QUIETO".equalsIgnoreCase(this.estado)
                || "Detenido".equalsIgnoreCase(this.estado);
    }

    public boolean estaRetrocediendo() {
        return "RETROCEDIENDO".equalsIgnoreCase(this.estado);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado no puede estar vacIo");
        }

        this.estado = estado;
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {

        if (velocidad < 0 || Float.isNaN(velocidad) || Float.isInfinite(velocidad)) {

            throw new IllegalArgumentException("La velocidad no puede ser negativa");
        }
        this.velocidad = velocidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        if (direccion == null || direccion.trim().isEmpty()) {
            throw new IllegalArgumentException("La direccion no puede estar vacia");
        }
        this.direccion = direccion;
    }

    public String getModoOperacion() {
        return modoOperacion;
    }

    public void setModoOperacion(String modoOperacion) {
        this.modoOperacion = modoOperacion;
    }
}
