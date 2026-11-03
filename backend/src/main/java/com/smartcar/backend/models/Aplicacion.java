package com.smartcar.backend.models;

public class Aplicacion {

    private boolean estadoConexion;
    private String comandoActual;

    public Aplicacion() {
    }

    public Aplicacion(boolean estadoConexion, String comandoActual) {
        this.estadoConexion = estadoConexion;
        this.comandoActual = comandoActual;
    }

    public void conectar() {
        this.estadoConexion = true;
    }

    public void enviarComando(String comando) {
        if(estadoConexion) {
            this.comandoActual = comando;
            System.out.println("Comando enviado: " + comando);
        } else {
            System.out.println("No se puede enviar comando. La aplicación no está conectada.");
        }
    }

    public void mostrarEstado() {
        if(estadoConexion) {
            System.out.println("Aplicación conectada.");
        } else {
            System.out.println("Aplicación no conectada.");
        }
    }

    public void mostrarDistanciaObjeto() {
        //aqui se mostrara la distancia recibida desde el controlador
    }

    public void tocarBocina() {
        enviarComando("Tocar Bocina");
    }

    public boolean getEstadoConexion() {
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