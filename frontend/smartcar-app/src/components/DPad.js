// Control circular: 4 sectores (adelante, derecha, reversa, izquierda) y un boton central de parada.
// Un solo "responder" calcula la direccion segun donde esta el dedo: se puede deslizar de una
// flecha a otra sin levantarlo. Al soltar se llama onDir(null).
import React, { useRef } from 'react';
import { View } from 'react-native';
import Svg, { Circle, Defs, G, Path, RadialGradient, Rect, Stop } from 'react-native-svg';
import { C } from '../theme';

const CX = 150;           // centro (coordenadas del diseno, 300 x 300)
const R_EXT = 140;
const R_INT = 62;
const R_HUB = 50;
const MEDIO = 42;         // medio ancho de cada sector en grados (deja 6 grados de separacion)
const VB = 324;           // viewBox -12..312 para que quepa el anillo exterior

function punto(r, grados) {
  const a = (grados * Math.PI) / 180;
  return `${(CX + r * Math.cos(a)).toFixed(2)} ${(CX + r * Math.sin(a)).toFixed(2)}`;
}

function sector(centro) {
  const a0 = centro - MEDIO;
  const a1 = centro + MEDIO;
  return `M${punto(R_EXT, a0)} A${R_EXT} ${R_EXT} 0 0 1 ${punto(R_EXT, a1)} `
    + `L${punto(R_INT, a1)} A${R_INT} ${R_INT} 0 0 0 ${punto(R_INT, a0)} Z`;
}

const SECTORES = [
  { dir: 'UP', centro: -90, icono: 'M6 15l6-6 6 6', ix: 150, iy: 49, etiqueta: 'Adelante' },
  { dir: 'RIGHT', centro: 0, icono: 'M9 6l6 6-6 6', ix: 251, iy: 150, etiqueta: 'Derecha' },
  { dir: 'DOWN', centro: 90, icono: 'M6 9l6 6 6-6', ix: 150, iy: 251, etiqueta: 'Reversa' },
  { dir: 'LEFT', centro: 180, icono: 'M15 6l-6 6 6 6', ix: 49, iy: 150, etiqueta: 'Izquierda' },
].map((s) => ({ ...s, d: sector(s.centro) }));

// Direccion para un punto (coordenadas del diseno). undefined = fuera del control.
function zona(x, y) {
  const dx = x - CX;
  const dy = y - CX;
  const r = Math.hypot(dx, dy);
  if (r > R_EXT + 16) return undefined;
  if (r <= R_HUB + 2) return 'STOP';
  const a = (Math.atan2(dy, dx) * 180) / Math.PI;
  if (a >= -135 && a < -45) return 'UP';
  if (a >= -45 && a < 45) return 'RIGHT';
  if (a >= 45 && a < 135) return 'DOWN';
  return 'LEFT';
}

export default function DPad({ size = 300, activo, off, onDir }) {
  const actual = useRef(null);
  const escala = size / VB;

  function aplicar(e, inicio) {
    const { locationX, locationY } = e.nativeEvent;
    const z = zona(locationX / escala - 12, locationY / escala - 12);
    if (z === undefined) return;                // fuera: se mantiene la orden actual
    if (!inicio && z === 'STOP' && actual.current !== 'STOP') return;   // pasar por el centro no frena
    if (z !== actual.current) {
      actual.current = z;
      onDir(z);
    }
  }
  function soltar() {
    if (actual.current === null) return;
    actual.current = null;
    onDir(null);
  }

  return (
    <View
      style={{ width: size, height: size, opacity: off ? 0.4 : 1, touchAction: 'none', userSelect: 'none',
        cursor: off ? 'default' : 'pointer' }}
      accessible accessibilityRole="adjustable" accessibilityLabel="Control de direccion"
      onStartShouldSetResponder={() => !off}
      onMoveShouldSetResponder={() => !off}
      onResponderTerminationRequest={() => false}
      onResponderGrant={(e) => aplicar(e, true)}
      onResponderMove={(e) => aplicar(e, false)}
      onResponderRelease={soltar}
      onResponderTerminate={soltar}
    >
      {/* El SVG no recibe toques: asi locationX/Y siempre son relativos a este contenedor */}
      <View style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, pointerEvents: 'none' }}>
        <Svg width={size} height={size} viewBox={`-12 -12 ${VB} ${VB}`}>
          <Defs>
            <RadialGradient id="hub" cx="0.5" cy="0.3" r="0.7">
              <Stop offset="0" stopColor="#2a2a2e" /><Stop offset="1" stopColor="#0f0f11" />
            </RadialGradient>
          </Defs>
          <Circle cx={CX} cy={CX} r={160} fill="none" stroke="rgba(255,255,255,0.07)" />
          {SECTORES.map((s) => {
            const on = activo === s.dir;
            return (
              <G key={s.dir}>
                <Path d={s.d} fill={on ? C.blue : C.card2} />
                <G transform={`translate(${s.ix - 14} ${s.iy - 14}) scale(${28 / 24})`}>
                  <Path d={s.icono} fill="none" stroke={on ? '#fff' : C.icon} strokeWidth={2.2}
                    strokeLinecap="round" strokeLinejoin="round" />
                </G>
              </G>
            );
          })}
          <Circle cx={CX} cy={CX} r={R_HUB} fill={activo === 'STOP' ? C.red : 'url(#hub)'}
            stroke={activo === 'STOP' ? 'transparent' : 'rgba(255,255,255,0.1)'} strokeWidth={1} />
          <Rect x={CX - 12} y={CX - 12} width={24} height={24} rx={5} fill={activo === 'STOP' ? '#fff' : C.icon} />
        </Svg>
      </View>
    </View>
  );
}
