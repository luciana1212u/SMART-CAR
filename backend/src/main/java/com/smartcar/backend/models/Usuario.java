package com.smartcar.backend.models;

public class Usuario {
    private String id;
    private String nombre;
    private String usuario;
    private String email;

    // Agregacion: el usuario puede tener instalada una aplicacion
    private Aplicacion aplicacion;

    /** La contrasena no existe en el backend: la gestiona Supabase Auth. */
    public Usuario(String id, String nombre, String usuario, String email) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getEmail() {
        return email;
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
