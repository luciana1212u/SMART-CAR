package com.smartcar.backend.excepciones;

/** R12: con bateria baja no se permite mover el carro. */
public class BateriaBajaException extends RuntimeException {
    public BateriaBajaException(int nivel, int minimo) {
        super("Bateria baja (" + nivel + "%, minimo " + minimo + "%). Carga el carro para moverlo");
    }
}
