package com.smartcar.backend.services;

import org.springframework.stereotype.Service;

import com.smartcar.backend.models.Motor;

@Service
public class MotorService {

    private Motor motorI;   // motor izquierdo
    private Motor motorD;   // motor derecho

    // Constructor que usa Spring: crea los dos motores detenidos
    public MotorService() {
        this(new Motor(false, "Detenido"), new Motor(false, "Detenido"));
    }

    // Constructor para pruebas
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

    // Tracción diferencial: para girar, cada rueda gira en sentido contrario
    public void girarIzquierda() {
        motorI.girarAtras();
        motorD.girarAdelante();
    }

    public void girarDerecha() {
        motorI.girarAdelante();
        motorD.girarAtras();
    }

    // Lo usan detenerCarro() y la regla de batería baja (R12)
    public void detenerMotores() {
        motorI.detener();
        motorD.detener();
    }

    public Motor getMotorI() {
        return motorI;
    }

    public Motor getMotorD() {
        return motorD;
    }
}
