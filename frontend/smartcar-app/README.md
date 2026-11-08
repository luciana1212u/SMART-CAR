# SmartCar: movil y web

Ejecutar los comandos desde `smartcar-app`. Requiere Node.js 22.13 o superior.
No hace falta crear otra carpeta ni copiar archivos.

Este es el frontend compartido para Android, iPhone y navegador. Las pantallas
usan React Native y React Native Web; los mismos controles consumen la API HTTP.
El Bluetooth sigue a cargo del backend en el PC.

La pantalla de control se adapta al ancho disponible: una columna en celular
y dos columnas a partir de 900 px (movimiento y velocidad junto a accesorios).
El inicio de sesion queda centrado y permite desplazarse al abrir el teclado.

```cmd
npm install
```

Navegador:

```cmd
npm run web
```

Android/iPhone con Expo Go (escanear el QR):

```cmd
npm start
```

El backend debe estar funcionando en otra terminal en el puerto 8080.
En el navegador del PC, usar `localhost:8080` como servidor.
En el celular, usar la IPv4 del PC, por ejemplo `192.168.1.20`;
ambos dispositivos deben estar en la misma red.
Las URLs completas (por ejemplo `https://api.ejemplo.com`) conservan su puerto.
Para un backend HTTP local, abrir tambien el frontend mediante HTTP.

Compilar los archivos web en `dist/`:

```cmd
npm run build:web
```

En PowerShell, si la politica de scripts bloquea npm, usar `npm.cmd` en lugar de `npm`.

`dist/` contiene la version web para un servidor de archivos estaticos.
La exportacion web no genera un APK ni una app iOS. Para usar el telefono durante
el desarrollo, abrir el proyecto con una version compatible de Expo Go.
Las funciones de registro, inicio de sesion, sensores y control requieren el backend;
no se incluyen datos simulados en el frontend.
