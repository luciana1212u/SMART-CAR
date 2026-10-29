package com.smartcar.backend.models;

public class Aplicacion {

    private boolean estadoConexion;
    private String comandoActual;

    // Constructor vacío
    public Aplicacion() {
    }

    // Constructor con parámetros
    public Aplicacion(boolean estadoConexion, String comandoActual) {
        this.estadoConexion = estadoConexion;
        this.comandoActual = comandoActual;
    }

    // Métodos
    public void conectar() {
        this.estadoConexion = true;
    }

    public void enviarComando(String comando) {
        this.comandoActual = comando;
    }

    public void mostrarEstado() {
        // Lógica para mostrar el estado en la interfaz
    }

    public void mostrarDistanciaObjeto() {
        // Lógica para mostrar la distancia del sensor
    }

    public void tocarBocina() {
        // Lógica para enviar señal de bocina
    }

    // Getters y Setters
    public boolean isEstadoConexion() {
        return estadoConexion;
    }

    public void setEstadoConexion(boolean estadoConexion) {
        this.estadoConexion = estadoConexion;
    }

    public String getComandoActual() {
        return comandoActual;
    }

    public void setComandoActual(String comandoActual) {
        this.comandoActual = comandoActual;
    }
}