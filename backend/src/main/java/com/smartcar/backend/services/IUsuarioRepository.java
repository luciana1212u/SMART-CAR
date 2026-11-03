package com.smartcar.backend.services;

import com.smartcar.backend.models.Usuario;

public interface IUsuarioRepository {
    void guardar(Usuario usuario);

    Usuario buscarPorNombreUsuario(String nombreUsuario);

    boolean existe(String nombreUsuario);
}
