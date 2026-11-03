package com.smartcar.backend.services;

public interface IPasswordHasher {
    String generarSalt();

    String hash(String password, String salt);

    boolean verificar(String password, String salt, String hashGuardado);
}
