package com.smartcar.backend.models;

public interface ISensor {
    float leerDatos();
    boolean estaActivo();
    void activar();
    void desactivar();
}
