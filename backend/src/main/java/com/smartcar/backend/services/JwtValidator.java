package com.smartcar.backend.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.smartcar.backend.excepciones.AuthException;
import com.smartcar.backend.models.Usuario;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Valida los access tokens de Supabase: firma (algoritmos fijados de antemano), emisor, audiencia y expiracion.
 * Con SUPABASE_JWT_SECRET usa HS256; si no, las claves publicas ES256/RS256 del JWKS del proyecto.
 */
public class JwtValidator {
    private final DefaultJWTProcessor<SecurityContext> procesador;

    public JwtValidator(String supabaseUrl, String secretoHs256) {
        String base = supabaseUrl.trim().replaceAll("/+$", "");
        DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
        if (secretoHs256 != null && !secretoHs256.isBlank()) {
            JWKSource<SecurityContext> fuente =
                    new ImmutableSecret<>(secretoHs256.trim().getBytes(StandardCharsets.UTF_8));
            p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, fuente));
        } else {
            try {
                JWKSource<SecurityContext> fuente = JWKSourceBuilder.create(
                        URI.create(base + "/auth/v1/.well-known/jwks.json").toURL()).retrying(true).build();
                p.setJWSKeySelector(new JWSVerificationKeySelector<>(
                        Set.of(JWSAlgorithm.ES256, JWSAlgorithm.RS256), fuente));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("SUPABASE_URL invalida", e);
            }
        }
        p.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<SecurityContext>(
                Set.of("authenticated"),
                new JWTClaimsSet.Builder().issuer(base + "/auth/v1").build(),
                Set.of("sub", "exp", "iss", "aud"), Set.of()));
        this.procesador = p;
    }

    public Usuario validar(String token) {
        if (token == null || token.isBlank()) throw AuthException.tokenInvalido();
        try {
            JWTClaimsSet claims = procesador.process(token, null);
            Object metaObj = claims.getClaim("user_metadata");
            Map<?, ?> meta = metaObj instanceof Map<?, ?> m ? m : Map.of();
            String correo = claims.getStringClaim("email");
            return new Usuario(claims.getSubject(), texto(meta.get("nombre")), texto(meta.get("usuario")),
                    correo == null ? "" : correo);
        } catch (BadJWTException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")) {
                throw AuthException.sesionExpirada();
            }
            throw AuthException.tokenInvalido();
        } catch (Exception e) {
            // Firma invalida, algoritmo no permitido, token mal formado o JWKS inaccesible: nunca se acepta
            throw AuthException.tokenInvalido();
        }
    }

    private static String texto(Object o) {
        return o == null ? "" : o.toString();
    }
}
