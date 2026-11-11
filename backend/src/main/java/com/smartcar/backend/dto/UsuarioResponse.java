package com.smartcar.backend.dto;

import com.smartcar.backend.models.Usuario;

/** Datos publicos del usuario. La contrasena nunca pasa por aqui: la guarda Supabase Auth. */
public record UsuarioResponse(String id, String nombre, String usuario, String email) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getUsuario(), u.getEmail());
    }
}
