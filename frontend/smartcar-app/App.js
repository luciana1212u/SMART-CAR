import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, StatusBar, View } from 'react-native';
import { ApiError, crearApi } from './src/api';
import { borrarSesion, cargarSesion } from './src/sesion';
import LoginScreen from './src/screens/LoginScreen';
import ControlScreen from './src/screens/ControlScreen';

// Dos pantallas, sin librerias de navegacion: si hay sesion -> control; si no -> login.
// Al abrir la app se restaura la sesion guardada (tokens de Supabase) si sigue siendo valida.
export default function App() {
  const [sesion, setSesion] = useState(null);   // { url, usuario }
  const [mensaje, setMensaje] = useState(null);
  const [restaurando, setRestaurando] = useState(true);

  useEffect(() => {
    let activo = true;
    (async () => {
      const guardada = await cargarSesion();
      if (guardada) {
        try {
          // Renueva solo el token si hace falta; confirma con el backend que la cuenta sigue valida
          const r = await crearApi(guardada.url).verificarSesion();
          if (activo) setSesion({ url: guardada.url, usuario: r.usuario });
        } catch (e) {
          if (e instanceof ApiError && e.status === 401) {
            await borrarSesion();          // sesion vencida o cerrada: toca iniciar de nuevo
          } else if (activo) {
            setSesion({ url: guardada.url, usuario: guardada.usuario });   // sin red: reintenta desde la pantalla de control
          }
        }
      }
      if (activo) setRestaurando(false);
    })();
    return () => { activo = false; };
  }, []);

  const salir = useCallback(async (motivo) => {
    if (sesion) await crearApi(sesion.url).cerrarSesion();   // cierra en Supabase y borra los tokens locales
    setSesion(null);
    setMensaje(motivo || null);
  }, [sesion]);

  if (restaurando) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <>
      <StatusBar barStyle="default" />
      {sesion
        ? <ControlScreen baseUrl={sesion.url} usuario={sesion.usuario} onSalir={salir} />
        : <LoginScreen mensajeInicial={mensaje} onSesionIniciada={(url, usuario) => { setMensaje(null); setSesion({ url, usuario }); }} />}
    </>
  );
}
