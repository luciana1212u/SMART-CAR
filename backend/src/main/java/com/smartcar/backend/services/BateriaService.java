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

    public boolean esBaja() {
        return bateria.obtenerNivel() >= 0 && bateria.obtenerNivel() < nivelMinimo;
    }

    public int getNivelMinimo() {
        return nivelMinimo;
    }

    public void setNivelMinimo(int nivelMinimo) {
        this.nivelMinimo = nivelMinimo;
    }
}
