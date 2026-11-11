package com.smartcar.backend.config;

import com.smartcar.backend.comunicacion.Esp32BleTransport;
import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.models.Carro;
import com.smartcar.backend.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Arma el grafo de objetos del UML: controladores -> servicios -> modelos -> ESP32. */
@Configuration
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public Carro carro() {
        return new Carro();
    }

    @Bean(destroyMethod = "desconectar")
    public IEsp32Transport esp32Transport(
            @Value("${smartcar.ble.nombre:SmartCar}") String nombre,
            @Value("${smartcar.ble.servicio}") String servicio,
            @Value("${smartcar.ble.rx}") String rx,
            @Value("${smartcar.ble.tx}") String tx,
            @Value("${smartcar.ble.escaneo-ms:5000}") int escaneoMs) {
        return new Esp32BleTransport(nombre, servicio, rx, tx, escaneoMs);
    }

    @Bean
    public SupabaseAuthClient supabaseAuthClient(@Value("${smartcar.supabase.url:}") String url,
                                                 @Value("${smartcar.supabase.anon-key:}") String anonKey) {
        return new SupabaseAuthClient(url, anonKey);
    }

    /** Usuarios y contrasenas viven en Supabase Auth; el backend solo valida los JWT que esta emite. */
    @Bean
    public IAuthService authService(SupabaseAuthClient supabase,
                                    @Value("${smartcar.supabase.jwt-secret:}") String jwtSecret,
                                    @Value("${smartcar.sesion.silencio-ms:3000}") long silencioMs) {
        JwtValidator validador = supabase.estaConfigurado()
                ? new JwtValidator(supabase.getUrl(), jwtSecret) : null;
        return new AuthService(supabase, validador, silencioMs, Clock.systemUTC());
    }

    @Bean
    public SensorService sensorService(Carro carro) {
        return new SensorService(carro.getSensorAproximidad(), carro.getSensorLuz());
    }

    @Bean
    public BateriaService bateriaService(Carro carro,
                                         @Value("${smartcar.bateria.nivel-minimo:20}") int nivelMinimo) {
        return new BateriaService(carro.getBateria(), nivelMinimo);
    }

    @Bean
    public MotorService motorService(Carro carro) {
        return new MotorService(carro.getMotorIzquierdo(), carro.getMotorDerecho());
    }

    @Bean
    public LedsService ledsService(Carro carro, SensorService sensores, IAuthService auth, IEsp32Transport t) {
        return new LedsService(carro.getLuces(), carro.getLedPlaca(), sensores, auth, t);
    }

    @Bean
    public BocinaService bocinaService(Carro carro, IAuthService auth, IEsp32Transport t) {
        return new BocinaService(carro.getBocina(), auth, t);
    }

    @Bean
    public EvasionService evasionService(MotorService motores, SensorService sensores) {
        return new EvasionService(motores, sensores);
    }

    @Bean
    public MicrocontroladorService microcontroladorService(Carro carro, SensorService sensores, LedsService leds,
            MotorService motores, BocinaService bocina, IAuthService auth, BateriaService bateria,
            EvasionService evasion, IEsp32Transport transporte) {
        MicrocontroladorService svc = new MicrocontroladorService(
                carro, sensores, leds, motores, bocina, auth, bateria, evasion, transporte);
        transporte.setOyente(svc);      // todo lo que diga la ESP32 llega al servicio
        return svc;
    }

    @Value("${smartcar.cors.origenes:*}")
    private String[] origenesCors;

    /** Permite la web (Expo Web / HTML de prueba). La app movil no usa CORS. Se autentica con Bearer, sin cookies. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origenesCors).allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
