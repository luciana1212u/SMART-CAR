import { useColorScheme } from 'react-native';

const claro = { fondo: '#eef2f1', panel: '#ffffff', texto: '#16302b', suave: '#5d726d', linea: '#cfdad7',
  acento: '#0b7a6b', sobreAcento: '#ffffff', alerta: '#b3261e', ok: '#1b8a4b' };
const oscuro = { fondo: '#0f1b19', panel: '#172825', texto: '#e3efec', suave: '#93aba5', linea: '#2a403c',
  acento: '#3fc2ad', sobreAcento: '#06201b', alerta: '#ff8a80', ok: '#5fd68a' };

export function useTema() {
  return useColorScheme() === 'dark' ? oscuro : claro;
}
