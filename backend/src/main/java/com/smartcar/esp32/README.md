# Cableado y firmware SmartCar


| Componente | GPIO |
| --- | --- |
| Motor izquierdo IN1 / IN2 | 4 / 5 |
| Motor derecho IN3 / IN4 | 6 / 7 |
| Sensor de luz analogico | 8 |
| Ultrasonido TRIG / ECHO | 15 / 16 |
| Buzzer | 11 |
| Luces | 19 / 20 |

Las constantes de luces siguen los valores ejecutables del codigo original
del usuario: 19 y 20. El comentario original que menciona 21 no coincide con
esas constantes. Si el segundo LED esta fisicamente en 21, ajustar `PIN_LUZ_2`.

GPIO19 y GPIO20 comparten el USB nativo de la ESP32-S3. Con este cableado,
seleccionar `USB CDC On Boot: Disabled` y usar un puerto/adaptador USB-UART para
programar y consultar el monitor serie, sin usar el conector USB nativo al
controlar las luces. Otra opcion de cableado es mover los LEDs a GPIO1/2 y
actualizar las constantes, si se necesita conservar el USB nativo.

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

Prueba fisica despues de cargar el firmware: seleccionar luces AUTO y comprobar
que se apagan al iluminar el LDR y se encienden al taparlo; probar tambien ON y
OFF. Verificar que el movimiento no se bloquea por el cambio de luz.
