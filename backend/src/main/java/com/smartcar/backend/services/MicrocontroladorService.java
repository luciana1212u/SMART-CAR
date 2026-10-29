package com.smartcar.backend.services;

import com.smartcar.backend.models.carro;
import org.springframework.stereotype.Service;

@Service
public class MicrocontroladorService {

    private final carro carro;
    private final SensorService sensorService;
    private final LedsService ledsService;
    private final MotorService motorService;
    private final BocinaService bocinaService;
    private final IAuthService authService;

    // Lo usan las notas del UML: recibirComando() lo guarda y ejecutarCiclo() lo ejecuta
    private volatile String comandoPendiente;

    public MicrocontroladorService(carro carro,
                                   SensorService sensorService,
                                   LedsService ledsService,
                                   MotorService motorService,
                                   BocinaService bocinaService,
                                   IAuthService authService) {
        this.carro = carro;
        this.sensorService = sensorService;
        this.ledsService = ledsService;
        this.motorService = motorService;
        this.bocinaService = bocinaService;
        this.authService = authService;
        this.comandoPendiente = null;
    }

    // solo si authService.estaAutenticado()
    public void recibirComando(String comando) {
        if (!authService.estaAutenticado()) {
            return; // R13: sin sesión el comando se ignora
        }
        if (comando == null) {
            return;
        }
        String normalizado = comando.trim().toUpperCase();
        if ("DETENER".equals(normalizado)) {
            detenerCarro(); // R10: DETENER siempre se ejecuta
        } else if (esMovimientoValido(normalizado)) {
            comandoPendiente = normalizado;
        }
    }

    public void ejecutarCiclo() {
        boolean hayObstaculo = sensorService.hayObstaculo();

        if (hayObstaculo) {
            detenerCarro(); // seguridad primero
        } else {
            ejecutarComandoPendiente();
        }

        ledsService.evaluarOscuridad();
        ledsService.actualizarLedPlaca(carro.estaQuieto());
        bocinaService.evaluarActivacion(hayObstaculo, carro.estaRetrocediendo());
    }

    public void detenerCarro() {
        comandoPendiente = null; // si no se limpia, el siguiente ciclo volvería a mover el carro
        motorService.detenerMotores();
        carro.cambiarEstado("QUIETO");
    }

    private boolean esMovimientoValido(String comando) {
        return "AVANZAR".equals(comando)
                || "RETROCEDER".equals(comando)
                || "IZQUIERDA".equals(comando)
                || "DERECHA".equals(comando);
    }

    private void ejecutarComandoPendiente() {
        String comando = comandoPendiente;
        if (comando == null) {
            return;
        }
        switch (comando) {
            case "AVANZAR" -> {
                motorService.avanzar();
                carro.cambiarEstado("AVANZANDO");
                carro.cambiarDireccion("ADELANTE");
            }
            case "RETROCEDER" -> {
                motorService.retroceder();
                carro.cambiarEstado("RETROCEDIENDO");
                carro.cambiarDireccion("ATRAS");
            }
            case "IZQUIERDA" -> {
                motorService.girarIzquierda();
                carro.cambiarEstado("AVANZANDO");
                carro.cambiarDireccion("IZQUIERDA");
            }
            case "DERECHA" -> {
                motorService.girarDerecha();
                carro.cambiarEstado("AVANZANDO");
                carro.cambiarDireccion("DERECHA");
            }
            default -> { }
        }
    }
}