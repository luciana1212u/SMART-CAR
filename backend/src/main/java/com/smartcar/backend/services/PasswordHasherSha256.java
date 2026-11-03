package com.smartcar.backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasherSha256 implements IPasswordHasher {
    private final String algoritmo = "SHA-256";

    @Override
    public String generarSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritmo);
            byte[] digest = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo no disponible: " + algoritmo, e);
        }
    }

    @Override
    public boolean verificar(String password, String salt, String hashGuardado) {
        return hash(password, salt).equals(hashGuardado);
    }
}
