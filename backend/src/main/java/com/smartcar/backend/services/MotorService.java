package com.smartcar.backend.services;

import com.smartcar.backend.models.Motor;

public class MotorService {
    private final Motor motorI;
    private final Motor motorD;

    public MotorService(Motor motorI, Motor motorD) {
        this.motorI = motorI;
        this.motorD = motorD;
    }

    // Codigos de movimiento del .ino: 0 parado, 1 adelante, 2 atras, 3 izquierda, 4 derecha
    public void aplicarMovimiento(int codigo) {
        switch (codigo) {
            case 1 -> avanzar();
            case 2 -> retroceder();
            case 3 -> girarIzquierda();
            case 4 -> girarDerecha();
            default -> detenerMotores();
        }
    }

    public void avanzar() {
        motorI.girarAdelante();
        motorD.girarAdelante();
    }

    public void retroceder() {
        motorI.girarAtras();
        motorD.girarAtras();
    }

    // Traccion diferencial: un motor adelante y el otro atras
    public void girarIzquierda() {
        motorI.girarAtras();
        motorD.girarAdelante();
    }

    public void girarDerecha() {
        motorI.girarAdelante();
        motorD.girarAtras();
    }

    public void detenerMotores() {
        motorI.detener();
        motorD.detener();
    }
}
