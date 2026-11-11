package com.smartcar.backend.dto;

/** Tokens de Supabase Auth que la app guarda para volver a entrar sin escribir la contrasena. */
public record SesionResponse(String accessToken, String refreshToken, long expiresIn, UsuarioResponse usuario) { }
