package com.smartcar.backend.repository;

import com.smartcar.backend.models.Usuario;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UsuarioRepository implements IUsuarioRepository {
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    @Override
    public void guardar(Usuario usuario) {
        usuarios.put(usuario.getUsuario(), usuario);
    }

    @Override
    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        return usuarios.get(nombreUsuario);
    }

    @Override
    public boolean existe(String nombreUsuario) {
        return usuarios.containsKey(nombreUsuario);
    }
}
