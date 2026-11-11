package com.smartcar.backend.dto;

/** Cuerpos JSON que envia la app. */
public final class Peticiones {
    private Peticiones() { }

    public record Registro(String nombre, String usuario, String email, String password) { }

    public record Login(String email, String password) { }

    public record Refresco(String refreshToken) { }

    public record Comando(String comando) { }

    public record Luces(boolean encender) { }

    public record Bocina(boolean activar) { }
}
