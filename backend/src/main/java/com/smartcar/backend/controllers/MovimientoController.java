package com.smartcar.backend.controllers;

import com.smartcar.backend.dto.Peticiones;
import com.smartcar.backend.services.MicrocontroladorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Comandos aceptados: AVANZAR, RETROCEDER, IZQUIERDA, DERECHA, DETENER,
 * MODO_AUTO, MODO_MANUAL, VEL:n (80-255), DIST:n (5-100).
 */
@RestController
@RequestMapping("/api/movimiento")
public class MovimientoController {
    private final MicrocontroladorService microcontroladorService;

    public MovimientoController(MicrocontroladorService microcontroladorService) {
        this.microcontroladorService = microcontroladorService;
    }

    @PostMapping
    public Map<String, Object> enviarMovimiento(@RequestBody Peticiones.Comando body) {
        microcontroladorService.recibirComando(body.comando());
        return Map.of("ok", true, "comando", body.comando());
    }

    @PostMapping("/detener")
    public Map<String, Object> detener() {
        microcontroladorService.exigirSesion();
        microcontroladorService.detenerCarro();
        return Map.of("ok", true, "comando", "DETENER");
    }
}
