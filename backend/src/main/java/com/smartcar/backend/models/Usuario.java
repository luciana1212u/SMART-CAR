package com.smartcar.backend.models;

public class Usuario {

    private Aplicacion aplicacion;
    private int id;
    private String nombre;

    public Usuario() {
    }

    public Usuario(Aplicacion aplicacion, int id, String nombre) {
        this.aplicacion = aplicacion;
        this.id = id;
        this.nombre = nombre;
    }

    public void controlarCarrito() {
        aplicacion.enviarComando("avanzar");    
        // Lógica para controlar el carrito ejemplo: avanzar, retroceder, girar, etc.
    }

    public void consultarEstado() {
        aplicacion.mostrarEstado();        
    }

    public Aplicacion getAplicacion() {
        return aplicacion;
    }

    public void setAplicacion(Aplicacion aplicacion) {
        this.aplicacion = aplicacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
