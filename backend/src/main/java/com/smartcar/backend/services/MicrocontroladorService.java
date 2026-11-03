package com.smartcar.backend.services;

import com.smartcar.backend.models.Carro;

public class MicrocontroladorService {
    private final Carro carro;
    private final SensorService sensorService;
    private final LedsService ledsService;
    private final MotorService motorService;
    private final BocinaService bocinaService;
    private final IAuthService authService;
    private final BateriaService bateriaService;
    private final EvasionService evasionService;
    private String comandoPendiente = "DETENER";

    public MicrocontroladorService(Carro carro,
                                   SensorService sensorService,
                                   LedsService ledsService,
                                   MotorService motorService,
                                   BocinaService bocinaService,
                                   IAuthService authService,
                                   BateriaService bateriaService,
                                   EvasionService evasionService) {
        this.carro = carro;
        this.sensorService = sensorService;
        this.ledsService = ledsService;
        this.motorService = motorService;
        this.bocinaService = bocinaService;
        this.authService = authService;
        this.bateriaService = bateriaService;
        this.evasionService = evasionService;
    }

    public void recibirComando(String comando) {
        // R13: sin sesion iniciada se ignora el comando
        if (!authService.estaAutenticado()) {
            return;
        }
        // R10: DETENER siempre se ejecuta
        if ("DETENER".equals(comando)) {
            detenerCarro();
        } else {
            comandoPendiente = comando;
        }
    }

    public void ejecutarCiclo() {
        // R12: bateria baja -> detener motores y salir
        if (bateriaService.esBaja()) {
            motorService.detenerMotores();
            carro.cambiarEstado("QUIETO");
            return;
        }
        // Seguridad primero: si hay obstaculo, evadir
        if (sensorService.hayObstaculo()) {
            evasionService.evadir();
        } else {
            switch (comandoPendiente) {
                case "AVANZAR":
                    motorService.avanzar();
                    carro.cambiarEstado("AVANZANDO");
                    break;
                case "RETROCEDER":
                    motorService.retroceder();
                    carro.cambiarEstado("RETROCEDIENDO");
                    break;
                case "IZQUIERDA":
                    motorService.girarIzquierda();
                    break;
                case "DERECHA":
                    motorService.girarDerecha();
                    break;
                default:
                    detenerCarro();
            }
        }
        ledsService.evaluarOscuridad();
        ledsService.actualizarLedPlaca(carro.estaQuieto());
        bocinaService.evaluarActivacion(sensorService.hayObstaculo(), carro.estaRetrocediendo());
    }

    public void detenerCarro() {
        comandoPendiente = "DETENER";
        motorService.detenerMotores();
        carro.cambiarEstado("QUIETO");
    }
}
