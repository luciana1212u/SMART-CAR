// SmartCar - control por Bluetooth (BLE) con ESP32-S3 N16R8, multitarea FreeRTOS
// Placa en Arduino IDE: "ESP32S3 Dev Module"
//   USB CDC On Boot: Disabled | Flash Size: 16MB | PSRAM: OPI PSRAM
//   Luces en GPIO19/20: no usar USB nativo (D-/D+); cargar mediante USB-UART.
//   Requiere el paquete "esp32 by Espressif" version 3.x (usa analogWrite)
//
// Quien le habla a esta placa ahora es el BACKEND (Spring Boot) por BLE; la app
// movil le habla al backend por HTTP. La ESP32 acepta UN solo cliente BLE a la vez:
// cierra la pagina HTML de prueba / apps BLE antes de levantar el backend.
//
// NUCLEO 0: tarea Comunicacion  -> BLE (recibir comandos / enviar telemetria y eventos)
// NUCLEO 1: tarea Sensores      -> ultrasonido (por interrupcion), luz, bateria
//           tarea Control       -> motores, luces, bocina, modo automatico
// Las tareas se comunican con colas (comandos y eventos); no hay delay() bloqueantes.
//
// Comandos (texto):
//   AVANZAR RETROCEDER IZQUIERDA DERECHA DETENER
//   LUCES_ON LUCES_OFF LUCES_AUTO  BOCINA_ON BOCINA_OFF
//   MODO_AUTO  MODO_MANUAL  VEL:n (80-255)  DIST:n (5-100 cm)  ESTADO
//   ON / OFF  -> LED RGB de la placa (el backend lo usa para indicar "carro quieto")
// Telemetria (3 paquetes rotando cada 100 ms; cada uno cabe en los 20 bytes de un
// paquete BLE con el MTU por defecto, no los alargues):
//   D:<cm>,L:<luz%>,B:<bateria%>
//   M:<A|M>,F:<luces 0|1>,H:<bocina 0-3>,X:<movimiento 0-4>
//   P:<pwm%>,S:-1,U:-1.0 (sin medidores); B:-1 = bateria no disponible
// Eventos: OK:<comando>  EV:OBSTACULO  EV:OBJ10  ERR:CMD
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define RX_UUID      "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define TX_UUID      "6e400003-b5a3-f393-cfa9-e50e24dcca9e"

// ---------------- PINES ----------------
const int IN1 = 4, IN2 = 5;        // Motor izquierdo
const int IN3 = 6, IN4 = 7;        // Motor derecho
const int PIN_LDR    = 8;          // Sensor de luz (ADC1)
// Sin medidor de bateria: no leer GPIO9 ni deducir carga desde el sensor de luz.
const int PIN_TRIG   = 15;
const int PIN_ECHO   = 16;         // OJO: el HC-SR04 saca 5 V; usa divisor a 3.3 V
const int PIN_BUZZER = 11;
const int PIN_LUZ_1  = 19;         // Cableado original del usuario
const int PIN_LUZ_2  = 20;
// true: GPIO -> resistencia -> LED -> GND. false: modulo/LED activo en LOW.
const bool LUCES_ACTIVAS_EN_ALTO = true;
// El cableado del LDR entrega mas voltaje en oscuridad.
const int LDR_ENCENDER_MV = 2550;  // oscuro: >2.5 V, margen para evitar parpadeos
const int LDR_APAGAR_MV = 2450;    // iluminado: <2.5 V
#define PIN_LED_RGB 48             // Algunas N16R8 (v1.1) traen el RGB en el GPIO 38: cambialo si no enciende

// ---------------- AJUSTES ----------------
int   distanciaMinima = 15;        // cm: por debajo hay obstaculo
int   velocidad       = 200;       // 0-255
const int ALERTA_CRITICA_CM = 10;  // alerta roja
const bool ALARMA_PROXIMIDAD = true;   // bocina siempre que haya un objeto cerca

// No hay encoders: el PWM es una orden de potencia, no una medida de cm/s.

const unsigned long REV_PERIODO_MS = 1000;   // un pitido corto cada segundo
const unsigned long REV_PITIDO_MS  = 200;
const unsigned long PROX_PERIODO_MS = 2000;  // 1 segundo ON + 1 segundo OFF
const unsigned long PROX_PITIDO_MS = 1000;

// ---------------- ESTADO ----------------
enum Modo { MANUAL, AUTO };
enum Mov  { PARADO, ADELANTE, ATRAS, IZQ, DER };
enum Est  { E_AVANCE, E_ALERTA, E_GIRO_A, E_MIDE_A, E_GIRO_B, E_MIDE_B, E_RETRO };
enum Luces { L_OFF, L_ON, L_AUTO };

// Escribe SOLO la tarea Control (las otras tareas solo leen)
volatile Modo modo = MANUAL;
volatile Mov  mov = PARADO;
volatile bool lucesEncendidas = false;
volatile bool bocinaManual = false;
volatile int modoBocina = 0;       // telemetria: 0 silencio, 1 manual, 2 reversa, 3 proximidad
Est   est = E_AVANCE;
Luces modoLuces = L_AUTO;
int   dirPrimera = 0;
unsigned long tEst = 0, tAtras = 0;
bool  criticoActivo = false;

// Escribe SOLO la tarea Sensores
volatile float distancia = 999;
volatile int   luzPct = 100;
volatile int   luzMv = 0;
const float voltaje = -1;        // -1 = medicion no disponible
const int bateriaPct = -1;

// Ultrasonido por interrupcion (sin pulseIn bloqueante)
volatile unsigned long echoInicio = 0, echoDur = 0;
volatile bool echoListo = false;
void IRAM_ATTR isrEcho() {
  if (digitalRead(PIN_ECHO)) echoInicio = micros();
  else { echoDur = micros() - echoInicio; echoListo = true; }
}

// Comunicacion
BLECharacteristic *tx;
volatile bool conectado = false;
QueueHandle_t colaComandos, colaEventos;   // mensajes de 32 bytes

void encolar(QueueHandle_t q, const char *txt) {
  char buf[32]; strncpy(buf, txt, 31); buf[31] = 0;
  xQueueSend(q, buf, 0);
}
void enviarEvento(const String &m) {
  Serial.println("-> " + m);
  if (conectado) encolar(colaEventos, m.c_str());
}
void led(uint8_t r, uint8_t g, uint8_t b) { neopixelWrite(PIN_LED_RGB, r, g, b); }

// ---------------- MOTORES ----------------
void detener()    { analogWrite(IN1, 0); analogWrite(IN2, 0); analogWrite(IN3, 0); analogWrite(IN4, 0); mov = PARADO; }
void avanzar()    { analogWrite(IN1, 0); analogWrite(IN2, velocidad); analogWrite(IN3, 0); analogWrite(IN4, velocidad); mov = ADELANTE; }
void retroceder() { if (mov != ATRAS) tAtras = millis(); analogWrite(IN1, velocidad); analogWrite(IN2, 0); analogWrite(IN3, velocidad); analogWrite(IN4, 0); mov = ATRAS; }
void girarDerecha()   { analogWrite(IN1, 0); analogWrite(IN2, velocidad); analogWrite(IN3, velocidad); analogWrite(IN4, 0); mov = DER; }
void girarIzquierda() { analogWrite(IN1, velocidad); analogWrite(IN2, 0); analogWrite(IN3, 0); analogWrite(IN4, velocidad); mov = IZQ; }

// ---------------- LUCES Y BOCINA (tarea Control) ----------------
void aplicarLuces(bool on) {
  const int nivel = (on == LUCES_ACTIVAS_EN_ALTO) ? HIGH : LOW;
  digitalWrite(PIN_LUZ_1, nivel);
  digitalWrite(PIN_LUZ_2, nivel);
  lucesEncendidas = on;
}
void actualizarLuces() {
  if (modoLuces == L_ON)       aplicarLuces(true);
  else if (modoLuces == L_OFF) aplicarLuces(false);
  else {
    int mv = luzMv;
    if (!lucesEncendidas && mv >= LDR_ENCENDER_MV) aplicarLuces(true);
    else if (lucesEncendidas && mv <= LDR_APAGAR_MV) aplicarLuces(false);
  }
}

// Prioridad: manual > reversa > limite de parada > aviso de proximidad.
// El aviso comienza al doble de DIST (40 cm por defecto), y es continuo
// al alcanzar DIST (20 cm). La reversa permite alejarse del objeto frontal.
void actualizarBocina() {
  static int patronAnterior = 0;
  static unsigned long inicioPatron = 0;
  unsigned long ahora = millis();
  const float d = distancia;
  int patron = 0;
  if (bocinaManual) patron = 1;
  else if (mov == ATRAS) patron = 2;
  else if (ALARMA_PROXIMIDAD && d > 0 && d <= distanciaMinima) patron = 4;
  else if (ALARMA_PROXIMIDAD && d > 0 && d <= distanciaMinima * 2) patron = 3;
  if (patron != patronAnterior) {
    inicioPatron = ahora;  // cada alerta nueva empieza sonando un periodo completo
    patronAnterior = patron;
  }
  bool suena = false;
  if (patron == 1 || patron == 4) suena = true;
  else if (patron == 2) suena = ((ahora - inicioPatron) % REV_PERIODO_MS) < REV_PITIDO_MS;
  else if (patron == 3) suena = ((ahora - inicioPatron) % PROX_PERIODO_MS) < PROX_PITIDO_MS;
  modoBocina = patron == 4 ? 3 : patron;
  digitalWrite(PIN_BUZZER, suena ? HIGH : LOW);
}

int pwmPct() { return (mov == PARADO) ? 0 : velocidad * 100 / 255; }
int velCms() { return -1; }  // sin sensor de velocidad
int hornEstado() {
  return modoBocina;
}

// ---------------- COMANDOS ----------------
void procesarComando(String cmd) {
  cmd.trim(); cmd.toUpperCase();
  Serial.println("<- " + cmd);

  if (cmd == "AVANZAR") {
    modo = MANUAL;
    if (distancia <= distanciaMinima) { detener(); enviarEvento("EV:OBSTACULO"); }
    else { avanzar(); enviarEvento("OK:AVANZAR"); }
  }
  else if (cmd == "RETROCEDER") { modo = MANUAL; retroceder();     enviarEvento("OK:RETROCEDER"); }
  else if (cmd == "IZQUIERDA")  { modo = MANUAL; girarIzquierda(); enviarEvento("OK:IZQUIERDA"); }
  else if (cmd == "DERECHA")    { modo = MANUAL; girarDerecha();   enviarEvento("OK:DERECHA"); }
  else if (cmd == "DETENER" || cmd == "MODO_MANUAL") { modo = MANUAL; detener(); enviarEvento("OK:" + cmd); }
  else if (cmd == "MODO_AUTO")  { modo = AUTO; est = E_AVANCE; enviarEvento("OK:MODO_AUTO"); }
  else if (cmd == "LUCES_ON")   { modoLuces = L_ON;   enviarEvento("OK:LUCES_ON"); }
  else if (cmd == "LUCES_OFF")  { modoLuces = L_OFF;  enviarEvento("OK:LUCES_OFF"); }
  else if (cmd == "LUCES_AUTO") { modoLuces = L_AUTO; enviarEvento("OK:LUCES_AUTO"); }
  else if (cmd == "BOCINA_ON")  { bocinaManual = true;  enviarEvento("OK:BOCINA_ON"); }
  else if (cmd == "BOCINA_OFF") { bocinaManual = false; enviarEvento("OK:BOCINA_OFF"); }
  else if (cmd.startsWith("VEL:")) {
    velocidad = constrain(cmd.substring(4).toInt(), 80, 255);
    if (mov == ADELANTE) avanzar(); else if (mov == ATRAS) retroceder();
    enviarEvento("OK:VEL=" + String(velocidad));
  }
  else if (cmd.startsWith("DIST:")) {
    distanciaMinima = constrain(cmd.substring(5).toInt(), 5, 100);
    enviarEvento("OK:DIST=" + String(distanciaMinima));
  }
  else if (cmd == "ON")  led(0, 40, 0);
  else if (cmd == "OFF") led(0, 0, 0);
  else if (cmd == "ESTADO") { }
  else enviarEvento("ERR:CMD");
}

// ---------------- MODO AUTOMATICO (maquina de estados) ----------------
void iniciarGiro(int dir) { if (dir == 0) girarDerecha(); else girarIzquierda(); }

void logicaAuto() {
  unsigned long ahora = millis();
  switch (est) {
    case E_AVANCE:
      if (distancia <= distanciaMinima) {
        detener();
        dirPrimera = random(0, 2);
        est = E_ALERTA; tEst = ahora;
        enviarEvento("EV:OBSTACULO");
      } else avanzar();
      break;
    case E_ALERTA:                                  // pausa con la bocina sonando
      if (ahora - tEst >= 400) { iniciarGiro(dirPrimera); est = E_GIRO_A; tEst = ahora; }
      break;
    case E_GIRO_A:
      if (ahora - tEst >= 500) { detener(); est = E_MIDE_A; tEst = ahora; }
      break;
    case E_MIDE_A:                                  // 150 ms = al menos una medida nueva
      if (ahora - tEst >= 150) {
        if (distancia > distanciaMinima) est = E_AVANCE;
        else { iniciarGiro(1 - dirPrimera); est = E_GIRO_B; tEst = ahora; }
      }
      break;
    case E_GIRO_B:
      if (ahora - tEst >= 1000) { detener(); est = E_MIDE_B; tEst = ahora; }
      break;
    case E_MIDE_B:
      if (ahora - tEst >= 150) {
        if (distancia > distanciaMinima) est = E_AVANCE;
        else { retroceder(); est = E_RETRO; tEst = ahora; }
      }
      break;
    case E_RETRO:
      if (ahora - tEst >= 400) { detener(); est = E_AVANCE; }
      break;
  }
}

// ================= TAREAS =================
// NUCLEO 1 - Control: ejecuta comandos, protege, decide y mueve
void tareaControl(void *p) {
  char buf[32];
  for (;;) {
    while (xQueueReceive(colaComandos, buf, 0) == pdTRUE) procesarComando(String(buf));

    float d = distancia;
    if (d < ALERTA_CRITICA_CM && !criticoActivo) { criticoActivo = true; enviarEvento("EV:OBJ10"); }
    if (d >= ALERTA_CRITICA_CM + 3) criticoActivo = false;

    if (modo == MANUAL && mov == ADELANTE && d <= distanciaMinima) {   // no choca en manual
      detener();
      enviarEvento("EV:OBSTACULO");
    }
    if (modo == AUTO) logicaAuto();

    actualizarLuces();
    actualizarBocina();
    vTaskDelay(pdMS_TO_TICKS(5));
  }
}

// NUCLEO 1 - Sensores: ultrasonido sin bloqueo, luz y bateria
void tareaSensores(void *p) {
  unsigned long tTrig = 0, tDisparo = 0, tLento = 0;
  bool esperando = false;
  for (;;) {
    unsigned long ms = millis();
    if (!esperando && ms - tTrig >= 60) {
      tTrig = ms; echoListo = false;
      digitalWrite(PIN_TRIG, HIGH); delayMicroseconds(10); digitalWrite(PIN_TRIG, LOW);  // pulso de 10 us
      tDisparo = micros(); esperando = true;
    }
    if (esperando) {
      if (echoListo) {
        float cm = echoDur * 0.0343 / 2.0;
        distancia = (cm > 0 && cm < 400) ? cm : 999.0;
        esperando = false;
      } else if (micros() - tDisparo > 30000) { distancia = 999.0; esperando = false; }  // sin eco
    }
    if (ms - tLento >= 200) {
      tLento = ms;
      uint32_t suma = 0;
      for (int i = 0; i < 8; ++i) suma += analogReadMilliVolts(PIN_LDR);
      luzMv = suma / 8;
      // Porcentaje relativo de iluminacion, no lux ni porcentaje de bateria.
      luzPct = constrain(100 - (int)(luzMv * 100L / 3300), 0, 100);
    }
    vTaskDelay(pdMS_TO_TICKS(2));
  }
}

// NUCLEO 0 - Comunicacion: envia eventos y telemetria por BLE
void tareaComunicacion(void *p) {
  unsigned long tTel = 0; int paso = 0; char msg[32];
  for (;;) {
    if (conectado) {
      while (xQueueReceive(colaEventos, msg, 0) == pdTRUE) {
        tx->setValue(msg); tx->notify();
        vTaskDelay(pdMS_TO_TICKS(10));
      }
      if (millis() - tTel >= 100) {
        tTel = millis(); paso = (paso + 1) % 3;
        float d = distancia; String t;
        if (paso == 0)      t = "D:" + String((int)min(d, 999.0f)) + ",L:" + String((int)luzPct) + ",B:" + String((int)bateriaPct);
        else if (paso == 1) t = String("M:") + (modo == AUTO ? "A" : "M") + ",F:" + (lucesEncendidas ? "1" : "0") +
                                ",H:" + String(hornEstado()) + ",X:" + String((int)mov);
        else                t = "P:" + String(pwmPct()) + ",S:" + String(velCms()) + ",U:" + String((float)voltaje, 1);
        tx->setValue(t.c_str()); tx->notify();
      }
    } else xQueueReset(colaEventos);
    vTaskDelay(pdMS_TO_TICKS(5));
  }
}

// ---------------- BLE ----------------
class Servidor : public BLEServerCallbacks {
  void onConnect(BLEServer *s) { conectado = true; led(0, 20, 0); Serial.println("Cliente conectado"); }
  void onDisconnect(BLEServer *s) {
    conectado = false; led(0, 0, 0);
    encolar(colaComandos, "DETENER");            // seguridad: se detiene al desconectar
    Serial.println("Cliente desconectado");
    BLEDevice::startAdvertising();
  }
};
class Comandos : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) { encolar(colaComandos, String(c->getValue().c_str()).c_str()); }
};

// ---------------- SETUP / LOOP ----------------
void setup() {
  Serial.begin(115200);
  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT); pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT);
  pinMode(PIN_TRIG, OUTPUT); pinMode(PIN_ECHO, INPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_BUZZER, LOW);
  // Preparar primero el nivel inactivo, tambien para modulos activos en LOW.
  digitalWrite(PIN_LUZ_1, LUCES_ACTIVAS_EN_ALTO ? LOW : HIGH);
  digitalWrite(PIN_LUZ_2, LUCES_ACTIVAS_EN_ALTO ? LOW : HIGH);
  pinMode(PIN_LUZ_1, OUTPUT); pinMode(PIN_LUZ_2, OUTPUT);
  aplicarLuces(false);
  analogReadResolution(12);
  analogSetPinAttenuation(PIN_LDR, ADC_11db);
  attachInterrupt(digitalPinToInterrupt(PIN_ECHO), isrEcho, CHANGE);
  detener();
  randomSeed(micros());
  led(0, 0, 0);

  colaComandos = xQueueCreate(8, 32);
  colaEventos  = xQueueCreate(12, 32);

  BLEDevice::init("SmartCar");
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new Servidor());
  BLEService *svc = server->createService(SERVICE_UUID);
  BLECharacteristic *rx = svc->createCharacteristic(
      RX_UUID, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  rx->setCallbacks(new Comandos());
  tx = svc->createCharacteristic(TX_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  tx->addDescriptor(new BLE2902());
  svc->start();
  BLEAdvertising *adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(SERVICE_UUID);
  adv->setScanResponse(true);
  BLEDevice::startAdvertising();

  //                       funcion            nombre         pila  param prio handle nucleo
  xTaskCreatePinnedToCore(tareaComunicacion, "Comunicacion", 6144, NULL, 1, NULL, 0);
  xTaskCreatePinnedToCore(tareaSensores,     "Sensores",     4096, NULL, 2, NULL, 1);
  xTaskCreatePinnedToCore(tareaControl,      "Control",      6144, NULL, 3, NULL, 1);
  Serial.println("SmartCar listo: anunciando por Bluetooth...");
}

void loop() { vTaskDelete(NULL); }   // todo el trabajo lo hacen las tareas
