package com.smartcar.backend.config;

import com.smartcar.backend.comunicacion.Esp32BleTransport;
import com.smartcar.backend.comunicacion.IEsp32Transport;
import com.smartcar.backend.models.Carro;
import com.smartcar.backend.repository.IUsuarioRepository;
import com.smartcar.backend.repository.UsuarioRepository;
import com.smartcar.backend.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
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
    public IUsuarioRepository usuarioRepository() {
        return new UsuarioRepository();
    }

    @Bean
    public IPasswordHasher passwordHasher() {
        return new PasswordHasherSha256();
    }

    @Bean
    public IAuthService authService(IUsuarioRepository repo, IPasswordHasher hasher) {
        return new AuthService(repo, hasher);
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

    /** Permite probar desde paginas web (como el HTML de prueba). La app movil no lo necesita. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
