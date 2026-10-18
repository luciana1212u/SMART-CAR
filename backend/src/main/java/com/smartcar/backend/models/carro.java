package com.smartcar.backend.models;

public class carro {

    private String estado;
    private float velocidad;
    private String direccion;

    public carro() {
        this.estado="Detenido";
        this.velocidad=0;
        this.direccion="Recto";
    }

    public carro(String estado, float velocidad, String direccion) {
        setEstado(estado);
        setVelocidad(velocidad);
        setDireccion(direccion);
    }

    public void avanzar() {
        this.estado="En movimiento";
    }

    public void detener() {
        this.velocidad = 0;
        this.estado= "Detenido";
    }

    public void cambiarDireccion(String nuevaDireccion) {
        setDireccion(nuevaDireccion);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        if (estado == null || estado.trim() .isEmpty()){
            throw new IllegalArgumentException("El estado no puede estar vacIo");
        }

        this.estado = estado;
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {

        if(velocidad < 0 || Float.isNaN(velocidad)|| Float.isInfinite(velocidad) ){

            throw new IllegalArgumentException("La velocidad no puede ser negativa");
        }
        this.velocidad = velocidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        if (direccion == null || direccion.trim() .isEmpty()){
            throw new IllegalArgumentException("La direccion no puede estar vacia");
        }
        this.direccion = direccion;
    }
}
