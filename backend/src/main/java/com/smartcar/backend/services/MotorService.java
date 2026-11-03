package com.smartcar.backend.services;

import com.smartcar.backend.models.Motor;

public class MotorService {
    private final Motor motorI;
    private final Motor motorD;

    public MotorService(Motor motorI, Motor motorD) {
        this.motorI = motorI;
        this.motorD = motorD;
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
