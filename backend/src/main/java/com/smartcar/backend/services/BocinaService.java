package com.smartcar.backend.services;

import com.smartcar.backend.models.Bocina;

public class BocinaService {
    private final Bocina bocina;

    public BocinaService(Bocina bocina) {
        this.bocina = bocina;
    }

    // Retrocediendo: pitido intermitente (1 por segundo). Obstaculo: pitido largo.
    public void evaluarActivacion(boolean hayObstaculo, boolean estaRetrocediendo) {
        if (estaRetrocediendo) {
            bocina.sonarIntermitente(1000);
        } else if (hayObstaculo) {
            bocina.sonarLargo();
        } else {
            bocina.silenciar();
        }
        bocina.actualizar(System.currentTimeMillis());
    }
}
