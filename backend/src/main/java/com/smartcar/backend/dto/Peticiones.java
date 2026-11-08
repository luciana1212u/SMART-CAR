package com.smartcar.backend.dto;

/** Cuerpos JSON que envia la app. */
public final class Peticiones {
    private Peticiones() { }

    public record Registro(String nombre, String usuario, String password) { }

    public record Login(String usuario, String password) { }

    public record Comando(String comando) { }

    public record Luces(boolean encender) { }

    public record Bocina(boolean activar) { }
}
