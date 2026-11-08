package com.smartcar.backend.controllers;

import com.smartcar.backend.dto.EstadoCarro;
import com.smartcar.backend.services.MicrocontroladorService;
import com.smartcar.backend.services.SensorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/estado")
public class EstadoController {
    private final SensorService sensorService;
    private final MicrocontroladorService microcontroladorService;

    public EstadoController(SensorService sensorService, MicrocontroladorService microcontroladorService) {
        this.sensorService = sensorService;
        this.microcontroladorService = microcontroladorService;
    }

    /** Foto completa: es la que usa la app para refrescar la pantalla. */
    @GetMapping
    public EstadoCarro consultarEstado() {
        microcontroladorService.exigirSesion();
        return microcontroladorService.consultarEstadoCompleto();
    }

    @GetMapping("/obstaculo")
    public Map<String, Object> consultarObstaculo() {
        microcontroladorService.exigirSesion();
        return Map.of("hayObstaculo", sensorService.hayObstaculo());
    }

    @GetMapping("/oscuridad")
    public Map<String, Object> consultarOscuridad() {
        microcontroladorService.exigirSesion();
        return Map.of("hayOscuridad", sensorService.hayOscuridad());
    }

    @GetMapping("/distancia")
    public Map<String, Object> consultarDistancia() {
        microcontroladorService.exigirSesion();
        return Map.of("distanciaCm", sensorService.consultarDistancia());
    }

    @GetMapping("/luz")
    public Map<String, Object> consultarNivelLuz() {
        microcontroladorService.exigirSesion();
        return Map.of("nivelLuz", sensorService.consultarNivelLuz());
    }

    @GetMapping("/bateria")
    public Map<String, Object> consultarBateria() {
        microcontroladorService.exigirSesion();
        return Map.of("bateriaPct", microcontroladorService.consultarBateria());
    }
}
