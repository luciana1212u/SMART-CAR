package com.smartcar.backend.services;

import com.smartcar.backend.models.Bateria;

public class BateriaService {
    private final Bateria bateria;
    private int nivelMinimo = 20;

    public BateriaService(Bateria bateria) {
        this.bateria = bateria;
    }

    public BateriaService(Bateria bateria, int nivelMinimo) {
        this.bateria = bateria;
        this.nivelMinimo = nivelMinimo;
    }

    // Lo llama MicrocontroladorService con los paquetes B:<%> y U:<voltios>
    public void actualizarNivel(int nivel) {
        bateria.actualizarNivel(nivel);
    }

    public void actualizarVoltaje(float voltaje) {
        bateria.setVoltaje(voltaje);
    }

    public int consultarNivel() {
        return bateria.obtenerNivel();
    }

    public float consultarVoltaje() {
        return bateria.getVoltaje();
    }
    
    public boolean esBaja() {
        return bateria.obtenerNivel() < nivelMinimo;
    }

    public int getNivelMinimo() {
        return nivelMinimo;
    }

    public void setNivelMinimo(int nivelMinimo) {
        this.nivelMinimo = nivelMinimo;
    }
}
