package com.smartcar.backend.models;

public class Usuario {
    private int id;
    private String nombre;
    private String usuario;
    private String passwordHash;
    private String salt;

    // Agregacion: el usuario puede tener instalada una aplicacion
    private Aplicacion aplicacion;

    public Usuario(int id, String nombre, String usuario, String passwordHash, String salt) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public Aplicacion getAplicacion() {
        return aplicacion;
    }

    public void setAplicacion(Aplicacion aplicacion) {
        this.aplicacion = aplicacion;
    }

    public void controlarCarrito(String comando) {
        if (aplicacion != null) {
            aplicacion.enviarComando(comando);
        }
    }

    public String consultarEstado() {
        return aplicacion != null ? aplicacion.mostrarEstado() : "SIN_APLICACION";
    }
}
