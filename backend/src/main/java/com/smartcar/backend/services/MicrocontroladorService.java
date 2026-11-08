package com.smartcar.backend.services;

import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.dto.EstadoCarro;
import com.smartcar.backend.excepciones.BateriaBajaException;
import com.smartcar.backend.excepciones.SesionNoIniciadaException;
import com.smartcar.backend.models.Carro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Envia ordenes y refleja el estado real recibido de la ESP32. */
public class MicrocontroladorService implements IEsp32Transport.Oyente {
    private static final Logger log = LoggerFactory.getLogger(MicrocontroladorService.class);
    private static final Set<String> COMANDOS = Set.of("AVANZAR", "RETROCEDER", "IZQUIERDA",
            "DERECHA", "DETENER", "MODO_AUTO", "MODO_MANUAL", "ESTADO");
    private final Carro carro;
    private final SensorService sensores;
    private final LedsService leds;
    private final MotorService motores;
    private final BocinaService bocina;
    private final IAuthService auth;
    private final BateriaService bateria;
    private final IEsp32Transport transporte;
    private final ArrayDeque<String> eventos = new ArrayDeque<>();
    private volatile long ultimaTelemetria;
    private volatile int pwm;
    private volatile String ultimoEvento = "";

    public MicrocontroladorService(Carro carro, SensorService sensores, LedsService leds,
            MotorService motores, BocinaService bocina, IAuthService auth,
            BateriaService bateria, EvasionService evasion, IEsp32Transport transporte) {
        this.carro = carro;
        this.sensores = sensores;
        this.leds = leds;
        this.motores = motores;
        this.bocina = bocina;
        this.auth = auth;
        this.bateria = bateria;
        this.transporte = transporte;
    }

    public void exigirSesion() {
        if (!auth.estaAutenticado()) throw new SesionNoIniciadaException();
    }

    public void recibirComando(String comando) {
        exigirSesion();
        if (comando == null || comando.isBlank()) {
            throw new IllegalArgumentException("Debes indicar un comando");
        }
        String c = comando.trim().toUpperCase(Locale.ROOT);
        if (!COMANDOS.contains(c)) {
            String[] partes = c.split(":", -1);
            if (partes.length != 2 || !(partes[0].equals("VEL") || partes[0].equals("DIST"))) {
                throw new IllegalArgumentException("Comando desconocido: " + c);
            }
            int valor;
            try { valor = Integer.parseInt(partes[1]); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("El comando requiere un numero entero");
            }
            int min = partes[0].equals("VEL") ? 80 : 5;
            int max = partes[0].equals("VEL") ? 255 : 100;
            if (valor < min || valor > max) {
                throw new IllegalArgumentException("El valor debe estar entre " + min + " y " + max);
            }
        }
        if (Set.of("AVANZAR", "RETROCEDER", "IZQUIERDA", "DERECHA", "MODO_AUTO").contains(c)
                && bateria.esBaja()) {
            throw new BateriaBajaException(consultarBateria(), bateria.getNivelMinimo());
        }
        transporte.enviar(c);
        if (c.startsWith("DIST:")) sensores.setUmbralDistancia(Integer.parseInt(c.substring(5)));
    }

    public void detenerCarro() {
        transporte.enviar("DETENER");
        reflejarMovimiento(0);
    }

    public void ejecutarCiclo() {
        if (!transporte.estaConectado()) return;
        if ((!auth.estaAutenticado() || bateria.esBaja()) && !carro.estaQuieto()) detenerCarro();
        leds.actualizarLedPlaca(carro.estaQuieto());
    }

    @Override
    public void onConectado() {
        ultimaTelemetria = 0;
        carro.getBateria().actualizarNivel(-1);
        carro.getBateria().setVoltaje(-1);
        leds.reiniciarLedPlaca();
    }

    @Override
    public synchronized void onDesconectado() {
        ultimaTelemetria = 0;
        carro.getBateria().actualizarNivel(-1);
        carro.getBateria().setVoltaje(-1);
        reflejarMovimiento(0);
        bocina.sincronizar(0);
        leds.reiniciarLedPlaca();
    }

    @Override
    public synchronized void onMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return;
        String texto = mensaje.trim();
        if (texto.startsWith("EV:") || texto.startsWith("OK:") || texto.startsWith("ERR:")) {
            ultimoEvento = texto;
            if (eventos.size() == 30) eventos.removeFirst();
            eventos.addLast(texto);
            return;
        }
        try {
            Map<String, String> campos = new HashMap<>();
            for (String campo : texto.split(",")) {
                String[] par = campo.split(":", 2);
                if (par.length != 2) throw new IllegalArgumentException("Campo incompleto");
                campos.put(par[0], par[1]);
            }
            if (campos.keySet().containsAll(Set.of("D", "L", "B"))) {
                float distancia = numero(campos.get("D"));
                float luz = numero(campos.get("L"));
                int nivel = Integer.parseInt(campos.get("B"));
                sensores.actualizarLecturas(distancia, luz);
                carro.getBateria().actualizarNivel(nivel);
            } else if (campos.keySet().containsAll(Set.of("M", "F", "H", "X"))) {
                int luces = Integer.parseInt(campos.get("F"));
                int sonido = Integer.parseInt(campos.get("H"));
                int movimiento = Integer.parseInt(campos.get("X"));
                carro.setModoOperacion("A".equals(campos.get("M")) ? "AUTO" : "MANUAL");
                leds.sincronizarLuces(luces == 1);
                bocina.sincronizar(sonido);
                reflejarMovimiento(movimiento);
            } else if (campos.keySet().containsAll(Set.of("P", "S", "U"))) {
                int potencia = Integer.parseInt(campos.get("P"));
                float velocidad = numero(campos.get("S"));
                float voltaje = numero(campos.get("U"));
                pwm = potencia;
                carro.setVelocidad(velocidad);
                carro.getBateria().setVoltaje(voltaje);
            } else return;
            ultimaTelemetria = System.currentTimeMillis();
        } catch (IllegalArgumentException e) {
            log.debug("Telemetria invalida: {}", texto);
        }
    }

    private static float numero(String texto) {
        float valor = Float.parseFloat(texto);
        if (!Float.isFinite(valor)) throw new IllegalArgumentException("Valor no finito");
        return valor;
    }

    private void reflejarMovimiento(int codigo) {
        motores.aplicarMovimiento(codigo);
        carro.cambiarEstado(switch (codigo) {
            case 1 -> "AVANZANDO";
            case 2 -> "RETROCEDIENDO";
            case 3 -> "GIRANDO_IZQUIERDA";
            case 4 -> "GIRANDO_DERECHA";
            default -> "QUIETO";
        });
        carro.cambiarDireccion(switch (codigo) {
            case 2 -> "ATRAS";
            case 3 -> "IZQUIERDA";
            case 4 -> "DERECHA";
            default -> "ADELANTE";
        });
        if (codigo == 0) { carro.setVelocidad(0); pwm = 0; }
    }

    public int consultarBateria() { return carro.getBateria().obtenerNivel(); }

    public synchronized EstadoCarro consultarEstadoCompleto() {
        return new EstadoCarro(transporte.estaConectado(), ultimaTelemetria > 0
                && System.currentTimeMillis() - ultimaTelemetria < 3000,
                carro.getModoOperacion(), carro.getEstado(), carro.getDireccion(), carro.getVelocidad(),
                pwm, sensores.consultarDistancia(), sensores.hayObstaculo(), sensores.consultarNivelLuz(),
                sensores.hayOscuridad(), consultarBateria(), carro.getBateria().getVoltaje(), bateria.esBaja(),
                leds.lucesEncendidas(), leds.getModoLuces(), bocina.estaSonando(), bocina.getPatron(),
                ultimoEvento, List.copyOf(eventos));
    }
}
