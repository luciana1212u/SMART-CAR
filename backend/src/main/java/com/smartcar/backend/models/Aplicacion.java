package com.smartcar.backend.models;

public class Aplicacion {

    private boolean estadoConexion;
    private String comandoActual;
    private Microcontrolador microcontrolador;

    public Aplicacion() {
    }

    public Aplicacion(boolean estadoConexion, String comandoActual, Microcontrolador microcontrolador) {
        this.microcontrolador = microcontrolador;
        this.estadoConexion = estadoConexion;
        this.comandoActual = comandoActual;
    }

    public boolean isEstadoConexion() {
        return estadoConexion;
    }

    public String getComandoActual() {
        return comandoActual;
    }

    public void conectar() {
        this.estadoConexion = true;
    }

    public void enviarComando(String comando) {
        if (!estadoConexion) {
            return;
        }
        this.comandoActual = comando;
        microcontrolador.recibirComando(comando);
    }

    public String mostrarEstado() {
        return microcontrolador.getEstado();
    }

    public float mostrarDistanciaObjeto(SensorAprox sensor) {
        return sensor.medirDistancia();
    }
}