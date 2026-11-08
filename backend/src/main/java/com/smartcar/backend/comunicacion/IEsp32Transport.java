package com.smartcar.backend.comunicacion;

/**
 * Canal de comunicacion entre el backend y la ESP32-S3 del carro.
 * El backend utiliza Bluetooth BLE para comunicarse con la ESP32 fisica.
 */
public interface IEsp32Transport {

    /** Quien quiere enterarse de lo que pasa en el canal (lo implementa MicrocontroladorService). */
    interface Oyente {
        /** Llego un mensaje de texto de la ESP32 (telemetria o evento). */
        void onMensaje(String mensaje);

        void onConectado();

        void onDesconectado();
    }

    void setOyente(Oyente oyente);

    /** Intenta enlazarse con la ESP32. No lanza excepcion si no la encuentra: solo queda desconectado. */
    void conectar();

    void desconectar();

    boolean estaConectado();

    /** Envia un comando de texto (ej. "AVANZAR"). Lanza CarroNoConectadoException si no hay enlace. */
    void enviar(String comando);
}
