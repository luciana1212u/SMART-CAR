// Sesion del usuario: tokens de Supabase (los entrega el backend) guardados en el dispositivo.
// Nunca se guarda la contrasena. En movil usa el almacen seguro del sistema (Keychain / Keystore);
// en web (donde SecureStore no existe) usa localStorage.
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const CLAVE = 'smartcar.sesion';

let actual = null;   // { url, accessToken, refreshToken, usuario } en memoria, compartido por todas las llamadas

async function escribir(valor) {
  try {
    if (Platform.OS === 'web') {
      if (valor === null) window.localStorage.removeItem(CLAVE);
      else window.localStorage.setItem(CLAVE, valor);
    } else if (valor === null) {
      await SecureStore.deleteItemAsync(CLAVE);
    } else {
      await SecureStore.setItemAsync(CLAVE, valor);
    }
  } catch { /* sin almacenamiento disponible: la sesion solo vive mientras la app este abierta */ }
}

async function leer() {
  try {
    return Platform.OS === 'web'
      ? window.localStorage.getItem(CLAVE)
      : await SecureStore.getItemAsync(CLAVE);
  } catch {
    return null;
  }
}

/** Restaura la sesion guardada (o null). */
export async function cargarSesion() {
  const texto = await leer();
  if (!texto) return null;
  try {
    const s = JSON.parse(texto);
    if (!s?.url || !s?.accessToken || !s?.refreshToken) return null;
    actual = s;
    return s;
  } catch {
    return null;
  }
}

export async function guardarSesion(sesion) {
  actual = sesion;
  await escribir(JSON.stringify(sesion));
}

/** Tras renovar los tokens (el refresh token rota en cada uso). */
export async function actualizarTokens(accessToken, refreshToken) {
  if (!actual) return;
  actual = { ...actual, accessToken, refreshToken };
  await escribir(JSON.stringify(actual));
}

export async function borrarSesion() {
  actual = null;
  await escribir(null);
}

export function tokens() {
  return actual ? { accessToken: actual.accessToken, refreshToken: actual.refreshToken } : null;
}
