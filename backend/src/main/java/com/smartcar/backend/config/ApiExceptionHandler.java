package com.smartcar.backend.config;

import com.smartcar.backend.excepciones.AuthException;
import com.smartcar.backend.excepciones.BateriaBajaException;
import com.smartcar.backend.excepciones.CarroNoConectadoException;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Traduce las excepciones del dominio a codigos HTTP que la app entiende. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SesionNoIniciadaException.class)
    public ResponseEntity<Map<String, String>> sinSesion(SesionNoIniciadaException e) {
        return responder(HttpStatus.UNAUTHORIZED, "SIN_SESION", e);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> errorAuth(AuthException e) {
        return responder(e.getEstado(), e.getCodigo(), e);
    }

    @ExceptionHandler(CarroNoConectadoException.class)
    public ResponseEntity<Map<String, String>> sinCarro(CarroNoConectadoException e) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "CARRO_NO_CONECTADO", e);
    }

    @ExceptionHandler(BateriaBajaException.class)
    public ResponseEntity<Map<String, String>> bateriaBaja(BateriaBajaException e) {
        return responder(HttpStatus.CONFLICT, "BATERIA_BAJA", e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> peticionInvalida(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, "PETICION_INVALIDA", e);
    }

    private ResponseEntity<Map<String, String>> responder(HttpStatus estado, String codigo, Exception e) {
        String mensaje = e.getMessage() == null ? codigo : e.getMessage();
        return ResponseEntity.status(estado).body(Map.of("error", codigo, "mensaje", mensaje));
    }
}
