import React, { useCallback, useState } from 'react';
import { StatusBar } from 'react-native';
import { crearApi } from './src/api';
import LoginScreen from './src/screens/LoginScreen';
import ControlScreen from './src/screens/ControlScreen';

// Dos pantallas, sin librerias de navegacion: si hay sesion -> control; si no -> login.
export default function App() {
  const [sesion, setSesion] = useState(null);   // { url, usuario }
  const [mensaje, setMensaje] = useState(null);

  const salir = useCallback(async (motivo) => {
    if (sesion) {
      try { await crearApi(sesion.url).cerrarSesion(); } catch { /* ya sin conexion */ }
    }
    setSesion(null);
    setMensaje(motivo || null);
  }, [sesion]);

  return (
    <>
      <StatusBar barStyle="default" />
      {sesion
        ? <ControlScreen baseUrl={sesion.url} usuario={sesion.usuario} onSalir={salir} />
        : <LoginScreen mensajeInicial={mensaje} onSesionIniciada={(url, usuario) => { setMensaje(null); setSesion({ url, usuario }); }} />}
    </>
  );
}
