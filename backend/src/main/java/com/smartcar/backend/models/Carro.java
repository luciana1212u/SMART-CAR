package com.smartcar.backend.models;

import java.util.ArrayList;
import java.util.List;

public class Carro {
    private String estado;
    private float velocidad;
    private String direccion;
    private String modoOperacion;

    // Composicion: el carro es el "todo", las partes no existen sin el
    private Microcontrolador microcontrolador;
    private final List<Motor> motores = new ArrayList<>();
    private Bateria bateria;
    private final List<Leds> leds = new ArrayList<>();
    private final List<Sensor> sensores = new ArrayList<>();
    private final Bocina bocina = new Bocina();

    public Carro() {
        this.estado = "QUIETO";
        this.velocidad = 0f;
        this.direccion = "ADELANTE";
        this.modoOperacion = "MANUAL";
        this.microcontrolador = new Microcontrolador();
        this.bateria = new Bateria(-1, -1f);
        motores.add(new Motor(1));
        motores.add(new Motor(2));
        leds.add(new Leds());
        leds.add(new Leds());
        sensores.add(new SensorAprox(1, 200f));
        sensores.add(new SensorLuz(2));
    }

    public Motor getMotorIzquierdo() { return motores.get(0); }
    public Motor getMotorDerecho() { return motores.get(1); }
    public Leds getLuces() { return leds.get(0); }
    public Leds getLedPlaca() { return leds.get(1); }
    public SensorAprox getSensorAproximidad() { return (SensorAprox) sensores.get(0); }
    public SensorLuz getSensorLuz() { return (SensorLuz) sensores.get(1); }
    public Bocina getBocina() { return bocina; }

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
