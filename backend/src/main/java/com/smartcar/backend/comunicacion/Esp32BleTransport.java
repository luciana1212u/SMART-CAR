package com.smartcar.backend.comunicacion;

import org.simplejavable.Adapter;
import org.simplejavable.BluetoothUUID;
import org.simplejavable.Peripheral;
import com.smartcar.backend.excepciones.CarroNoConectadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Transporte real: Bluetooth Low Energy (BLE) hacia la ESP32-S3, usando SimpleJavaBLE.
 * El backend actua como "central" (cliente GATT) y la ESP32 como "periferico".
 *
 * UUIDs identicos a los del .ino y a los de la pagina HTML de prueba:
 *   RX (escritura)    : backend -> ESP32 (comandos)
 *   TX (notificacion) : ESP32 -> backend (telemetria y eventos)
 */
public class Esp32BleTransport implements IEsp32Transport {
    private static final Logger log = LoggerFactory.getLogger(Esp32BleTransport.class);

    private final String prefijoNombre;
    private final BluetoothUUID servicio;
    private final BluetoothUUID rx;
    private final BluetoothUUID tx;
    private final int tiempoEscaneoMs;

    private Adapter adaptador;
    private volatile Peripheral periferico;
    private volatile boolean conectado = false;
    private volatile Oyente oyente;

    public Esp32BleTransport(String prefijoNombre, String uuidServicio, String uuidRx,
                             String uuidTx, int tiempoEscaneoMs) {
        this.prefijoNombre = prefijoNombre;
        this.servicio = new BluetoothUUID(uuidServicio);
        this.rx = new BluetoothUUID(uuidRx);
        this.tx = new BluetoothUUID(uuidTx);
        this.tiempoEscaneoMs = tiempoEscaneoMs;
    }

    @Override
    public void setOyente(Oyente oyente) {
        this.oyente = oyente;
    }

    @Override
    public synchronized void conectar() {
        if (conectado) {
            return;
        }
        try {
            if (adaptador == null) {
                List<Adapter> adaptadores = Adapter.getAdapters();
                if (adaptadores.isEmpty()) {
                    log.warn("No se encontro ningun adaptador Bluetooth en este equipo.");
                    return;
                }
                adaptador = adaptadores.get(0);
            }

            log.info("Buscando dispositivos BLE cuyo nombre empieza por '{}' ({} ms)...", prefijoNombre, tiempoEscaneoMs);
            adaptador.scanFor(tiempoEscaneoMs);

            Peripheral objetivo = null;
            for (Peripheral p : adaptador.scanGetResults()) {
                String nombre = p.getIdentifier();
                boolean disponible = p.isConnectable();
                log.info("Dispositivo BLE detectado: '{}', conectable={}", nombre, disponible);
                if (nombre != null && nombre.startsWith(prefijoNombre) && disponible) {
                    objetivo = p;
                    break;
                }
            }
            if (objetivo == null) {
                log.info("ESP32 no encontrada. (esta encendida? hay otro equipo conectado a ella?)");
                return;
            }

            // El listener se registra ANTES de conectar para no perder el evento de desconexion.
            objetivo.setEventListener(new Peripheral.EventListener() {
                @Override
                public void onDisconnected() {
                    marcarDesconectado("la ESP32 se desconecto");
                }
            });
            periferico = objetivo;
            objetivo.connect();
            objetivo.notify(servicio, tx, datos -> {
                Oyente o = oyente;
                if (o != null) {
                    o.onMensaje(new String(datos, StandardCharsets.UTF_8).trim());
                }
            });

            periferico = objetivo;
            conectado = true;
            log.info("Conectado a la ESP32 '{}'.", objetivo.getIdentifier());
            Oyente o = oyente;
            if (o != null) {
                o.onConectado();
            }
        } catch (Exception | LinkageError e) {
            log.warn("No se pudo conectar con la ESP32: {}", e.getMessage());
            limpiar();
            marcarDesconectado("fallo al conectar");
        }
    }

    @Override
    public synchronized void desconectar() {
        limpiar();
        marcarDesconectado("desconexion solicitada");
    }

    @Override
    public boolean estaConectado() {
        return conectado;
    }

    @Override
    public synchronized void enviar(String comando) {
        Peripheral p = periferico;
        if (!conectado || p == null) {
            throw new CarroNoConectadoException("La ESP32 no esta conectada por Bluetooth");
        }
        try {
            p.writeRequest(servicio, rx, comando.getBytes(StandardCharsets.UTF_8));
            log.debug("-> {}", comando);
        } catch (Exception e) {
            marcarDesconectado("error al escribir: " + e.getMessage());
            throw new CarroNoConectadoException("Fallo el envio por Bluetooth: " + e.getMessage());
        }
    }

    private void limpiar() {
        Peripheral p = periferico;
        if (p != null) {
            try {
                if (p.isConnected()) {
                    p.disconnect();
                }
            } catch (Exception ignorada) {
                // ya estaba caida
            }
        }
    }

    private void marcarDesconectado(String motivo) {
        boolean estabaConectado = conectado;
        conectado = false;
        periferico = null;
        if (estabaConectado) {
            log.warn("Enlace BLE perdido: {}", motivo);
            Oyente o = oyente;
            if (o != null) {
                o.onDesconectado();
            }
        }
    }
}
