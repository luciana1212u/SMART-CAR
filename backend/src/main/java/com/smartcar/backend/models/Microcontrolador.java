package com.smartcar.backend.models;

public class Microcontrolador {
    private String estado;
    private float velocidadActual;
    private String modoOperacion;
    private String comandoActual;

    public Microcontrolador() {
        this.estado = "QUIETO";
        this.velocidadActual = 0f;
        this.modoOperacion = "MANUAL";
        this.comandoActual = "DETENER";
    }

    public String getEstado() {
        return estado;
    }

    public float getVelocidadActual() {
        return velocidadActual;
    }

    public String getModoOperacion() {
        return modoOperacion;
    }

    public void setModoOperacion(String modoOperacion) {
        this.modoOperacion = modoOperacion;
    }

    public void procesarDatos() {
        // Punto de extension: aqui se procesan los datos leidos de los sensores
    }

    public void ejecutarComando() {
        switch (comandoActual) {
            case "AVANZAR":
                this.estado = "AVANZANDO";
                this.velocidadActual = 1f;
                break;
            case "RETROCEDER":
                this.estado = "RETROCEDIENDO";
                this.velocidadActual = 1f;
                break;
            default:
                detenerCarro();
        }
    }

    public void detenerCarro() {
        this.estado = "QUIETO";
        this.velocidadActual = 0f;
        this.comandoActual = "DETENER";
    }

    public void recibirComando(String comando) {
        this.comandoActual = comando;
    }

    public void controlarIluminacion() {
        // La logica de iluminacion vive en LedsService
    }

    public void monitorearSeguridad() {
        // La logica de seguridad vive en SensorService / EvasionService
    }

    public void controlarMotor() {
        // La logica de motores vive en MotorService
    }

    public void leerSensores() {
        // La lectura de sensores vive en SensorService
    }
}
