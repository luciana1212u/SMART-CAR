package com.smartcar.backend.controllers;

import com.smartcar.backend.dto.Peticiones;
import com.smartcar.backend.services.BocinaService;
import com.smartcar.backend.services.LedsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accesorios")
public class AccesoriosController {
    private final LedsService ledsService;
    private final BocinaService bocinaService;

    public AccesoriosController(LedsService ledsService, BocinaService bocinaService) {
        this.ledsService = ledsService;
        this.bocinaService = bocinaService;
    }

    @PutMapping("/luces")
    public Map<String, Object> controlarLuces(@RequestBody Peticiones.Luces body) {
        ledsService.controlarLuces(body.encender());
        return Map.of("ok", true, "modoLuces", ledsService.getModoLuces());
    }

    @PostMapping("/luces/auto")
    public Map<String, Object> luzAutomatica() {
        ledsService.luzAutomatica();
        return Map.of("ok", true, "modoLuces", ledsService.getModoLuces());
    }

    @PutMapping("/bocina")
    public Map<String, Object> controlarBocina(@RequestBody Peticiones.Bocina body) {
        bocinaService.controlarBocina(body.activar());
        return Map.of("ok", true);
    }
}
