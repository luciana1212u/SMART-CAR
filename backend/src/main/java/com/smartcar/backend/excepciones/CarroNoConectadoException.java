package com.smartcar.backend.excepciones;

/** El backend no tiene enlace Bluetooth con la ESP32 en este momento. */
public class CarroNoConectadoException extends RuntimeException {
    public CarroNoConectadoException(String mensaje) {
        super(mensaje);
    }
}
