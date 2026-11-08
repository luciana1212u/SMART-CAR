package com.smartcar.backend.services;

import com.smartcar.backend.models.Usuario;
import com.smartcar.backend.repository.IUsuarioRepository;

public class AuthService implements IAuthService {
    private final IUsuarioRepository repositorio;
    private final IPasswordHasher hasher;
    private volatile Usuario usuarioAutenticado;
    private int intentosFallidos;
    private int siguienteId = 1;

    public AuthService(IUsuarioRepository repositorio, IPasswordHasher hasher) {
        this.repositorio = repositorio;
        this.hasher = hasher;
        this.intentosFallidos = 0;
    }

    @Override
    public synchronized Usuario registrar(String nombre, String nombreUsuario, String password) {
        if (nombre == null || nombre.isBlank() || nombreUsuario == null || nombreUsuario.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Nombre, usuario y contrasena son obligatorios");
        }
        if (repositorio.existe(nombreUsuario)) {
            throw new IllegalArgumentException("El usuario ya existe: " + nombreUsuario);
        }
        String salt = hasher.generarSalt();
        String hash = hasher.hash(password, salt);
        Usuario usuario = new Usuario(siguienteId++, nombre, nombreUsuario, hash, salt);
        repositorio.guardar(usuario);
        return usuario;
    }

    @Override
    public boolean iniciarSesion(String nombreUsuario, String password) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Usuario y contrasena son obligatorios");
        }
        Usuario usuario = repositorio.buscarPorNombreUsuario(nombreUsuario);
        if (usuario != null && hasher.verificar(password, usuario.getSalt(), usuario.getPasswordHash())) {
            this.usuarioAutenticado = usuario;
            this.intentosFallidos = 0;
            return true;
        }
        this.intentosFallidos++;
        return false;
    }

    @Override
    public void cerrarSesion() {
        this.usuarioAutenticado = null;
    }

    @Override
    public boolean estaAutenticado() {
        return usuarioAutenticado != null;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }
}
