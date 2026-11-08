package com.smartcar.backend.dto;

import java.util.List;

/** Foto completa del carro que recibe la app en cada consulta. */
public record EstadoCarro(
        boolean esp32Conectada,
        boolean telemetriaActiva,
        String modo,              // MANUAL | AUTO
        String estado,            // QUIETO | AVANZANDO | RETROCEDIENDO | GIRANDO_IZQUIERDA | GIRANDO_DERECHA
        String direccion,
        float velocidadCms,
        int pwmPct,
        float distanciaCm,
        boolean hayObstaculo,
        float nivelLuz,
        boolean hayOscuridad,
        int bateriaPct,
        float voltaje,
        boolean bateriaBaja,
        boolean lucesEncendidas,
        String modoLuces,         // AUTO | ON | OFF
        boolean bocinaSonando,
        String bocinaPatron,      // APAGADA | LARGO | INTERMITENTE
        String ultimoEvento,
        List<String> eventos) {
}
