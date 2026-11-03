package com.smartcar.backend.models;

import java.util.ArrayList;
import java.util.List;

public class Carro {

    private String estado = "QUIETO"; // QUIETO, AVANZANDO, RETROCEDIENDO, GIRANDO
    private float velocidad;
    private String direccion = "RECTO";
    private String modoOperacion = "AUTONOMO"; // AUTONOMO, MANUAL

    // Composicion: el carro es el "todo", las partes no existen sin el
    private Microcontrolador microcontrolador;
    private final List<Motor> motores = new ArrayList<>();
    private Bateria bateria;
    private final List<Leds> leds = new ArrayList<>();
    private final List<Sensor> sensores = new ArrayList<>();

    public Carro() {
        this.estado = "QUIETO";
        this.velocidad = 0f;
        this.direccion = "ADELANTE";
        this.modoOperacion = "MANUAL";
        this.microcontrolador = new Microcontrolador();
        this.bateria = new Bateria(100, 12f);
    }

    public void cambiarEstado(String estado) {
        this.estado = estado;
    }

    public void cambiarDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean estaQuieto() {
        return "QUIETO".equals(estado);
    }

    public boolean estaRetrocediendo() {
        return "RETROCEDIENDO".equals(estado);
    }

    public String getEstado() {
        return estado;
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

    public String getModoOperacion() {
        return modoOperacion;
    }

    public void setModoOperacion(String modoOperacion) {
        this.modoOperacion = modoOperacion;
    }

    public Microcontrolador getMicrocontrolador() {
        return microcontrolador;
    }

    public Bateria getBateria() {
        return bateria;
    }

    public List<Motor> getMotores() {
        return motores;
    }

    public List<Leds> getLeds() {
        return leds;
    }

    public List<Sensor> getSensores() {
        return sensores;
    }

}
