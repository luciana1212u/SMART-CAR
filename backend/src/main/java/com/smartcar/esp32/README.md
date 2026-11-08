# Cableado y firmware SmartCar


| Componente | GPIO |
| --- | --- |
| Motor izquierdo IN1 / IN2 | 4 / 5 |
| Motor derecho IN3 / IN4 | 6 / 7 |
| Sensor de luz analogico | 8 |
| Ultrasonido TRIG / ECHO | 15 / 16 |
| Buzzer | 11 |
| Luces | 19 / 20 |


En Arduino IDE: ESP32S3 Dev Module, Flash Size 16MB, PSRAM OPI, paquete ESP32 3.x.
Si Arduino pide que el sketch este en una carpeta llamada SmartCar, aceptar
crear esa carpeta o copiar el archivo a `SmartCar/SmartCar.ino`.

En modo AUTO, el LDR controla solamente las luces: encender desde 2.55 V y
apagar hasta 2.45 V. El margen alrededor de 2.5 V evita parpadeos. Se promedian
ocho lecturas y se configura la atenuacion del ADC. Los modos ON y OFF manuales
prevalecen sobre AUTO. `LUCES_ACTIVAS_EN_ALTO` debe coincidir con el circuito LED.
El porcentaje L representa iluminacion relativa, no una medida calibrada en lux.

El hardware no tiene medidor de bateria. No se lee GPIO9: `B:-1` y `U:-1.0`
significan no disponible. El backend no confunde ese estado con bateria baja,
y la interfaz muestra Sin medidor. No existe proteccion de descarga basada en
software para una bateria que no se mide.

El porcentaje P es el PWM aplicado (0 al detenerse); no mide velocidad fisica.
`S:-1` indica que no hay lectura de velocidad. Para mostrar cm/s reales hacen
falta sensores, por ejemplo encoders, y calibracion de rueda/transmision.


