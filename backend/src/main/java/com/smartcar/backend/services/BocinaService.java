package com.smartcar.backend.services;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import com.smartcar.backend.models.Bocina;

public class BocinaService {
    private final Bocina bocina;
    private final IAuthService authService;
    private final IEsp32Transport transporte;

    public BocinaService(Bocina bocina, IAuthService authService, IEsp32Transport transporte) {
        this.bocina = bocina;
        this.authService = authService;
        this.transporte = transporte;
    }

     // Bocina manual desde la app. La automatica (retroceso: pitido intermitente,
    // obstaculo: alarma de proximidad) la decide la ESP32 y llega por telemetria.
    public void controlarBocina(boolean activar) {
        if (!authService.estaAutenticado()) {
            throw new SesionNoIniciadaException();
        }
        transporte.enviar(activar ? "BOCINA_ON" : "BOCINA_OFF");
    }

    // Campo H de la telemetria: 0 apagada, 1 manual, 2 retroceso, 3 proximidad
    public void sincronizar(int codigoH) {
        bocina.sincronizar(codigoH);
    }

    public boolean estaSonando() {
        return bocina.isEstaSonando();
    }

    public String getPatron() {
        return bocina.getPatron();
    }
}
