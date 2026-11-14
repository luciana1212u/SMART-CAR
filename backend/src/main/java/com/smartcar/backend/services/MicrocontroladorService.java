package com.smartcar.backend.services;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.dto.EstadoCarro;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import com.smartcar.backend.models.Carro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Nucleo del backend: es el "Microcontrolador" visto desde el servidor.
 *  - Hacia la ESP32: valida las ordenes de la app y las envia por Bluetooth.
 *  - Desde la ESP32: recibe la telemetria y actualiza el modelo del carro.
 *  - Ciclo de supervision: aplica las reglas R10 y R13.
 */
public class MicrocontroladorService implements IEsp32Transport.Oyente {
    private static final Logger log = LoggerFactory.getLogger(MicrocontroladorService.class);

    private static final Pattern COMANDO_VALIDO = Pattern.compile(
            "AVANZAR|RETROCEDER|IZQUIERDA|DERECHA|DETENER|MODO_AUTO|MODO_MANUAL|DIST:\\d{1,3}");
    private static final long TELEMETRIA_VIGENTE_MS = 1500;
    private static final long REINTENTO_DETENER_MS = 1000;
    private static final int MAX_EVENTOS = 20;

    private final Carro carro;
    private final SensorService sensorService;
    private final LedsService ledsService;
    private final MotorService motorService;
    private final BocinaService bocinaService;
    private final IAuthService authService;
    private final EvasionService evasionService;
    private final IEsp32Transport transporte;

    private volatile String comandoPendiente = "DETENER";
    private volatile long ultimaTelemetriaMs = 0;
    private volatile long ultimoDetenerForzadoMs = 0;
    private volatile String ultimoEvento = "";
    private final Deque<String> eventos = new ArrayDeque<>();

    public MicrocontroladorService(Carro carro, SensorService sensorService,
                                   LedsService ledsService, MotorService motorService, BocinaService bocinaService,
                                   IAuthService authService,
                                   EvasionService evasionService, IEsp32Transport transporte) {
        this.carro = carro;
        this.sensorService = sensorService;
        this.ledsService = ledsService;
        this.motorService = motorService;
        this.bocinaService = bocinaService;
        this.authService = authService;
        this.evasionService = evasionService;
        this.transporte = transporte;
    }

    // =====================================================================
    //  APP -> BACKEND -> ESP32
    // =====================================================================

    public void exigirSesion() {
        if (!authService.estaAutenticado()) {
            throw new SesionNoIniciadaException();
        }
    }

    public void recibirComando(String comando) {
        // R13: sin sesion iniciada se rechaza el comando
        exigirSesion();
        String c = comando == null ? "" : comando.trim().toUpperCase();
        if (!COMANDO_VALIDO.matcher(c).matches()) {
            throw new IllegalArgumentException("Comando no valido: " + comando);
        }

        // R10: DETENER siempre se ejecuta
        if ("DETENER".equals(c)) {
            detenerCarro();
            return;
        }

        if (c.startsWith("DIST:")) {
            int d = Integer.parseInt(c.substring(5));
            if (d < 5 || d > 100) {
                throw new IllegalArgumentException("DIST debe estar entre 5 y 100 cm");
            }
            transporte.enviar(c);
            sensorService.setUmbralDistancia(d);     // el umbral del backend sigue al de la ESP32
        } else if ("MODO_AUTO".equals(c)) {
            evasionService.activar();
        } else if ("MODO_MANUAL".equals(c)) {
            evasionService.desactivar();
        } else {
            // AVANZAR / RETROCEDER / IZQUIERDA / DERECHA
            transporte.enviar(c);
        }
        comandoPendiente = c;
        carro.getMicrocontrolador().recibirComando(c);
    }

    public void detenerCarro() {
        comandoPendiente = "DETENER";
        carro.getMicrocontrolador().recibirComando("DETENER");
        transporte.enviar("DETENER");
    }

    // =====================================================================
    //  CICLO DE SUPERVISION (lo invoca SupervisorTask cada 250 ms)
    //  La ESP32 ya se protege sola (obstaculo, desconexion); aqui van las
    //  reglas que dependen de la sesion.
    // =====================================================================

    public void ejecutarCiclo() {
        if (!transporte.estaConectado()) {
            return;
        }
        try {
            boolean sinSesion = !authService.estaAutenticado();       // R13
            if (sinSesion && !carro.estaQuieto()) {
                long ahora = System.currentTimeMillis();
                if (ahora - ultimoDetenerForzadoMs >= REINTENTO_DETENER_MS) {
                    ultimoDetenerForzadoMs = ahora;
                    log.info("Supervision: sin sesion -> DETENER");
                    detenerCarro();
                }
            }
            ledsService.actualizarLedPlaca(carro.estaQuieto());
        } catch (RuntimeException e) {
            log.debug("Ciclo de supervision omitido: {}", e.getMessage());
        }
    }

    // =====================================================================
    //  ESP32 -> BACKEND  (telemetria y eventos)
    // =====================================================================

    @Override
    public void onConectado() {
        ledsService.reiniciarLedPlaca();
        registrarEvento("EV:ENLACE_BLE_OK");
    }

    @Override
    public void onDesconectado() {
        ultimaTelemetriaMs = 0;
        bocinaService.sincronizar(0);
        motorService.detenerMotores();
        carro.cambiarEstado("QUIETO");
        carro.setVelocidad(0f);
        carro.getMicrocontrolador().actualizarEstado("QUIETO", 0f, carro.getModoOperacion());
        registrarEvento("EV:ENLACE_BLE_PERDIDO");
    }

    @Override
    public void onMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return;
        }
        if (mensaje.startsWith("OK:") || mensaje.startsWith("EV:") || mensaje.startsWith("ERR:")) {
            registrarEvento(mensaje);
            return;
        }
        try {
            procesarDatos(parsear(mensaje));
            ultimaTelemetriaMs = System.currentTimeMillis();
        } catch (RuntimeException e) {
            log.warn("Paquete descartado '{}': {}", mensaje, e.getMessage());
        }
    }

    // Paquetes: D:<cm>,L:<luz%>,B:<bat%>  |  M:<A|M>,F:<0|1>,H:<0-3>,X:<0-4>  |  P:<pwm%>,S:<cm/s>,U:<voltios>
    private void procesarDatos(Map<String, String> kv) {
        if (kv.containsKey("D")) {
            sensorService.actualizarLecturas(Float.parseFloat(kv.get("D")), Float.parseFloat(kv.get("L")));
            // Se ignora la bateria de firmware antiguo.
        } else if (kv.containsKey("M")) {
            String modo = "A".equals(kv.get("M")) ? "AUTO" : "MANUAL";
            carro.setModoOperacion(modo);
            ledsService.sincronizarLuces("1".equals(kv.get("F")));
            bocinaService.sincronizar(Integer.parseInt(kv.get("H")));
            aplicarMovimiento(Integer.parseInt(kv.get("X")));
        } else if (kv.containsKey("P")) {
            // Paquete antiguo de potencia, velocidad y voltaje: ignorado.
        } else {
            throw new IllegalArgumentException("paquete desconocido");
        }
    }

    private void aplicarMovimiento(int x) {
        motorService.aplicarMovimiento(x);
        switch (x) {
            case 1 -> { carro.cambiarEstado("AVANZANDO"); carro.cambiarDireccion("ADELANTE"); }
            case 2 -> { carro.cambiarEstado("RETROCEDIENDO"); carro.cambiarDireccion("ATRAS"); }
            case 3 -> { carro.cambiarEstado("GIRANDO_IZQUIERDA"); carro.cambiarDireccion("IZQUIERDA"); }
            case 4 -> { carro.cambiarEstado("GIRANDO_DERECHA"); carro.cambiarDireccion("DERECHA"); }
            default -> carro.cambiarEstado("QUIETO");
        }
        carro.getMicrocontrolador().actualizarEstado(carro.getEstado(), carro.getVelocidad(), carro.getModoOperacion());
    }

    private static Map<String, String> parsear(String paquete) {
        Map<String, String> kv = new HashMap<>();
        for (String par : paquete.split(",")) {
            int i = par.indexOf(':');
            if (i > 0) {
                kv.put(par.substring(0, i).trim(), par.substring(i + 1).trim());
            }
        }
        return kv;
    }

    private void registrarEvento(String evento) {
        ultimoEvento = evento;
        synchronized (eventos) {
            eventos.addLast(evento);
            while (eventos.size() > MAX_EVENTOS) {
                eventos.removeFirst();
            }
        }
        log.info("ESP32: {}", evento);
    }

    // =====================================================================
    //  CONSULTAS (EstadoController)
    // =====================================================================

    public int consultarBateria() {
        return -1;
    }

    public EstadoCarro consultarEstadoCompleto() {
        List<String> copia;
        synchronized (eventos) {
            copia = new ArrayList<>(eventos);
        }
        boolean vigente = System.currentTimeMillis() - ultimaTelemetriaMs < TELEMETRIA_VIGENTE_MS;
        return new EstadoCarro(
                transporte.estaConectado(),
                vigente,
                carro.getModoOperacion(),
                carro.getEstado(),
                carro.getDireccion(),
                -1f,
                0,
                sensorService.consultarDistancia(),
                sensorService.hayObstaculo(),
                sensorService.consultarNivelLuz(),
                sensorService.hayOscuridad(),
                -1,
                -1f,
                false,
                ledsService.lucesEncendidas(),
                ledsService.getModoLuces(),
                bocinaService.estaSonando(),
                bocinaService.getPatron(),
                ultimoEvento,
                copia);
    }

    public String getComandoPendiente() {
        return comandoPendiente;
    }
}
