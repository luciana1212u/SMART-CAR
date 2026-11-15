# Frontend de SmartCar

El frontend React Native/Expo se desarrolla fuera de las fuentes Java del backend.

```text
SMART-CAR/
  backend/                  Spring Boot y API HTTP
  frontend/
    smartcar-app/           Aplicacion completa para web, Android e iOS
      src/screens/          LoginScreen y ControlScreen
      src/components/       CarView (carro SVG), DPad (control circular), ui (botones, tarjetas)
      src/theme.js          Paleta oscura del diseno
```

El diseno se adapta solo: en pantallas anchas (>= 1000 px) usa dos columnas; en el
celular, una sola. Dibujos con `react-native-svg`, asi que se ve igual en web, Android e iOS.
En web: teclas W A S D o flechas para mover, Espacio detiene, H bocina, L luces.

Para ejecutar la aplicacion, desde la raiz `SMART-CAR`:

```powershell
cd frontend/smartcar-app
npm.cmd ci
npm.cmd run web
```

Para abrirla en Expo Go, ejecutar `npm.cmd start` en la misma carpeta.
El backend debe ejecutarse por separado y utiliza exclusivamente la ESP32 fisica por BLE.

Las dependencias y las compilaciones generadas no se copian. `npm.cmd ci` instala
las dependencias fijadas en `package-lock.json`.



