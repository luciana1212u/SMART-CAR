
package com.smartcar.backend.services;

import com.smartcar.backend.models.Motor;
import org.springframework.stereotype.Service;

@Service
public class MotorService {

    private final Motor motorI;
    private final Motor motorD;
    private String estado = "DETENER";

    public MotorService() {
        this.motorI = new Motor(1);
        this.motorD = new Motor(2);
    }

    public void avanzar() {
        motorI.girarAdelante();
        motorD.girarAdelante();
        estado = "AVANZAR";
    }

    public void retroceder() {
        motorI.girarAtras();
        motorD.girarAtras();
        estado = "RETROCEDER";
    }

    public void girarIzquierda() {
        motorI.girarAtras();
        motorD.girarAdelante();
        estado = "IZQUIERDA";
    }

    public void girarDerecha() {
        motorI.girarAdelante();
        motorD.girarAtras();
        estado = "DERECHA";
    }

    public void detenerMotores() {
        motorI.detener();
        motorD.detener();
        estado = "DETENER";
    }

    public String getEstado() {
        return estado;
    }

    public Motor getMotorI() {
        return motorI;
    }

    public Motor getMotorD() {
        return motorD;
    }
}
