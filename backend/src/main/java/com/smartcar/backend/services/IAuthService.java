package com.smartcar.backend.services;

import com.smartcar.backend.models.Usuario;

public interface IAuthService {
    Usuario registrar(String nombre, String nombreUsuario, String password);

    boolean iniciarSesion(String nombreUsuario, String password);

    void cerrarSesion();

    boolean estaAutenticado();
}
