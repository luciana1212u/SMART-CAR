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
    private final SensorAprox sensorAproximidad = new SensorAprox(1, 400f);
    private final SensorLuz sensorLuz = new SensorLuz(2);
    private final Leds luces = new Leds();      // luces delanteras
    private final Leds ledPlaca = new Leds();   // LED de la placa: indica carro quieto
    private final Bocina bocina = new Bocina();

    public Carro() {
        this.estado = "QUIETO";
        this.velocidad = 0f;
        this.direccion = "ADELANTE";
        this.modoOperacion = "MANUAL";
        this.microcontrolador = new Microcontrolador();
        this.bateria = new Bateria(100, 12f);
        this.motores.add(new Motor(1));   // izquierdo
        this.motores.add(new Motor(2));   // derecho
        this.sensores.add(sensorAproximidad);
        this.sensores.add(sensorLuz);
        this.leds.add(luces);
        this.leds.add(ledPlaca);
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
    public Motor getMotorIzquierdo() {
        return motores.get(0);
    }

    public Motor getMotorDerecho() {
        return motores.get(1);
    }

    public SensorAprox getSensorAproximidad() {
        return sensorAproximidad;
    }

    public SensorLuz getSensorLuz() {
        return sensorLuz;
    }

    public Leds getLuces() {
        return luces;
    }

    public Leds getLedPlaca() {
        return ledPlaca;
    }

    public Bocina getBocina() {
        return bocina;
    }
}
