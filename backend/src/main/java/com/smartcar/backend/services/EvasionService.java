package com.smartcar.backend.services;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.models.Carro;

/**
 * Modo automatico con evasion de obstaculos.
 * La maquina de estados (detener, alerta, girar, medir, retroceder) corre DENTRO de la ESP32
 * porque necesita reaccionar en milisegundos; el backend solo la activa o desactiva.
 */
public class EvasionService {
    private final Carro carro;
    private final IEsp32Transport transporte;

    public EvasionService(Carro carro, IEsp32Transport transporte) {
        this.carro = carro;
        this.transporte = transporte;
    }

    public void activar() {
        transporte.enviar("MODO_AUTO");
        carro.setModoOperacion("AUTO");
    }

    public void desactivar() {
        transporte.enviar("MODO_MANUAL");   // la ESP32 tambien detiene los motores
        carro.setModoOperacion("MANUAL");
    }

    public boolean estaActivo() {
        return "AUTO".equals(carro.getModoOperacion());
    }
}
