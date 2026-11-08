package com.smartcar.backend.excepciones;

/** R13: sin sesion iniciada no se aceptan ordenes para el carro. */
public class SesionNoIniciadaException extends RuntimeException {
    public SesionNoIniciadaException() {
        super("Debes iniciar sesion para controlar el carro");
    }
}
