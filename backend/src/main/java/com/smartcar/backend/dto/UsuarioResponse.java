package com.smartcar.backend.dto;

import com.smartcar.backend.models.Usuario;

/** Nunca se expone el hash ni la sal. */
public record UsuarioResponse(int id, String nombre, String usuario) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getUsuario());
    }
}
