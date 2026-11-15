import { Platform } from 'react-native';

// Paleta unica (oscura) del diseno "RC control". Los nombres antiguos (fondo, panel, texto...)
// se mantienen para que cualquier pantalla vieja siga funcionando.
export const C = {
  bg: '#0a0a0b',
  card: '#141416',
  card2: '#1e1e21',
  hover: '#2a2a2e',
  hover2: '#3a3a40',
  line: 'rgba(255,255,255,0.07)',
  line2: 'rgba(255,255,255,0.14)',
  tx: '#f2f2f3',
  mut: '#8b8d93',
  icon: '#c9cbd0',
  blue: '#3e6ae1',
  blueSoft: '#9fb4ff',
  red: '#e82127',
  g: '#30d158',
  y: '#ffd60a',
  r: '#ff453a',
};

const tema = {
  fondo: C.bg, panel: C.card, texto: C.tx, suave: C.mut, linea: C.line2,
  acento: C.blue, sobreAcento: '#ffffff', alerta: C.r, ok: C.g,
};

export function useTema() {
  return tema;
}

// Inter en web (se carga en App.js); en el celular, la fuente del sistema.
export const FUENTE = Platform.OS === 'web' ? "'Inter', system-ui, sans-serif" : undefined;

// Estilo base de texto para no repetir la fuente en cada <Text>
export const txt = FUENTE ? { fontFamily: FUENTE } : {};
