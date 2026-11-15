// Vista superior del carro (SVG) con zonas de proximidad, faros, luz de reversa y ondas de bocina.
// Traducido del diseno rc-control.html a react-native-svg: funciona igual en web, Android e iOS.
// Los filtros de desenfoque del HTML se reemplazan por degradados radiales (mas compatibles y livianos).
import React, { useEffect, useState } from 'react';
import Svg, { Circle, ClipPath, Defs, Ellipse, G, LinearGradient, Path, RadialGradient, Rect, Stop } from 'react-native-svg';
import { C } from '../theme';

const CUERPO = 'M200 150C232 150 252 156 259 172C265 186 267 200 267 214C267 232 262 250 260 270C259 292 268 312 268 335C268 360 266 384 258 398C250 410 228 414 200 414C172 414 150 410 142 398C134 384 132 360 132 335C132 312 141 292 140 270C138 250 133 232 133 214C133 200 135 186 141 172C148 156 168 150 200 150Z';

// Valor que oscila entre 0 y 1 mientras `activo` (para pulsos sin depender de animaciones nativas)
function useOnda(activo, periodoMs) {
  const [f, setF] = useState(0);
  useEffect(() => {
    if (!activo) { setF(0); return undefined; }
    const inicio = Date.now();
    const id = setInterval(() => setF(((Date.now() - inicio) % periodoMs) / periodoMs), 40);
    return () => clearInterval(id);
  }, [activo, periodoMs]);
  return f;
}

function Banda({ d, color, encendida, pulso }) {
  const f = useOnda(encendida && pulso, 1000);
  // Pulso rojo: 1 -> 0.5 -> 1 (como el keyframe "alternate" del HTML)
  const op = !encendida ? 0.2 : pulso ? 1 - 0.5 * (1 - Math.abs(1 - 2 * f)) : 1;
  return (
    <G opacity={op}>
      {encendida && <Path d={d} fill="none" stroke={color} strokeWidth={26} strokeLinecap="round" opacity={0.18} />}
      <Path d={d} fill="none" stroke={color} strokeWidth={14} strokeLinecap="round" />
    </G>
  );
}

function OndasBocina({ activa }) {
  const f = useOnda(activa, 600);
  if (!activa) return null;
  const r = 90 * (0.6 + 0.65 * f);
  return <Circle cx={200} cy={280} r={r} fill="none" stroke={C.y} strokeWidth={2} opacity={0.8 * (1 - f)} />;
}

const ZONAS = {
  f: ['M78.8 135 A140 140 0 0 1 321.2 135', 'M103 149 A112 112 0 0 1 297 149', 'M127.3 163 A84 84 0 0 1 272.7 163'],
  r: ['M78.8 425 A140 140 0 0 0 321.2 425', 'M103 411 A112 112 0 0 0 297 411', 'M127.3 397 A84 84 0 0 0 272.7 397'],
};
const COLORES = [C.g, C.y, C.r];

function Zonas({ lado, nivel }) {
  return ZONAS[lado].map((d, i) => (
    <Banda key={i} d={d} color={COLORES[i]} encendida={nivel >= i + 1} pulso={i === 2} />
  ));
}

/**
 * props:
 *  luces       faros encendidos
 *  reversa     luz blanca de reversa
 *  bocina      ondas amarillas
 *  nivelFrente 0 libre, 1 verde, 2 amarilla, 3 roja; null = sin zonas
 *  nivelAtras  igual; null = el carro no tiene sensor trasero
 */
// Recuadro visible del dibujo [x, y, ancho, alto]. Ajustado al carro y sus zonas
// (con margen para el brillo de la curva verde, que antes quedaba cortada arriba).
export function cajaCarro(conZonas, conAtras) {
  if (!conZonas) return [100, 60, 200, 380];
  return [62, 44, 276, conAtras ? 396 : 382];
}

export default function CarView({ luces = false, reversa = false, bocina = false, nivelFrente = null, nivelAtras = null, style }) {
  const viewBox = cajaCarro(nivelFrente !== null || nivelAtras !== null, nivelAtras !== null).join(' ');
  return (
    <Svg viewBox={viewBox} preserveAspectRatio="xMidYMid meet" style={style}
      accessibilityRole="image" accessibilityLabel="Vista superior del carro con zonas de proximidad">
      <Defs>
        <LinearGradient id="paint" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor="#7d838e" /><Stop offset="0.18" stopColor="#c4c8cf" />
          <Stop offset="0.42" stopColor="#f4f5f7" /><Stop offset="0.5" stopColor="#ffffff" />
          <Stop offset="0.58" stopColor="#f4f5f7" /><Stop offset="0.82" stopColor="#c4c8cf" />
          <Stop offset="1" stopColor="#7d838e" />
        </LinearGradient>
        <LinearGradient id="shade" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#000" stopOpacity="0.22" /><Stop offset="0.18" stopColor="#000" stopOpacity="0" />
          <Stop offset="0.8" stopColor="#000" stopOpacity="0" /><Stop offset="1" stopColor="#000" stopOpacity="0.32" />
        </LinearGradient>
        <LinearGradient id="sheen" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#fff" stopOpacity="0.55" /><Stop offset="0.35" stopColor="#fff" stopOpacity="0" />
        </LinearGradient>
        <LinearGradient id="glass" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#3a4252" /><Stop offset="0.3" stopColor="#151a22" />
          <Stop offset="0.7" stopColor="#0d1016" /><Stop offset="1" stopColor="#2a313d" />
        </LinearGradient>
        <LinearGradient id="roof" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#1c2230" /><Stop offset="0.5" stopColor="#090b10" /><Stop offset="1" stopColor="#161b26" />
        </LinearGradient>
        <LinearGradient id="refl" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor="#9fb4ff" stopOpacity="0" /><Stop offset="0.5" stopColor="#9fb4ff" stopOpacity="0.28" />
          <Stop offset="1" stopColor="#9fb4ff" stopOpacity="0" />
        </LinearGradient>
        <LinearGradient id="tire" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor="#050506" /><Stop offset="0.5" stopColor="#26272b" /><Stop offset="1" stopColor="#050506" />
        </LinearGradient>
        <LinearGradient id="beam" gradientUnits="userSpaceOnUse" x1="0" y1="165" x2="0" y2="70">
          <Stop offset="0" stopColor="#eaf1ff" stopOpacity="0.45" /><Stop offset="1" stopColor="#eaf1ff" stopOpacity="0" />
        </LinearGradient>
        <RadialGradient id="sombra" cx="0.5" cy="0.5" r="0.5">
          <Stop offset="0" stopColor="#000" stopOpacity="0.85" /><Stop offset="0.7" stopColor="#000" stopOpacity="0.55" />
          <Stop offset="1" stopColor="#000" stopOpacity="0" />
        </RadialGradient>
        <RadialGradient id="piso" cx="0.5" cy="0.5" r="0.5">
          <Stop offset="0" stopColor={C.blue} stopOpacity="0.6" /><Stop offset="0.75" stopColor={C.blue} stopOpacity="0.25" />
          <Stop offset="1" stopColor={C.blue} stopOpacity="0" />
        </RadialGradient>
        <ClipPath id="bodyClip"><Path d={CUERPO} /></ClipPath>
      </Defs>

      {/* haces de los faros */}
      {luces && (
        <G>
          <Path d="M156 166 L92 70 L192 70 L184 158 Z" fill="url(#beam)" />
          <Path d="M244 166 L308 70 L208 70 L216 158 Z" fill="url(#beam)" />
        </G>
      )}

      {nivelFrente !== null && <Zonas lado="f" nivel={nivelFrente} />}
      {nivelAtras !== null && <Zonas lado="r" nivel={nivelAtras} />}
      <OndasBocina activa={bocina} />

      {/* sombra y luz de piso */}
      <Ellipse cx={200} cy={286} rx={112} ry={172} fill="url(#sombra)" />
      {luces && <Ellipse cx={200} cy={284} rx={104} ry={164} fill="url(#piso)" />}

      {/* llantas */}
      <Rect x={123} y={190} width={20} height={48} rx={7} fill="url(#tire)" />
      <Rect x={257} y={190} width={20} height={48} rx={7} fill="url(#tire)" />
      <Rect x={121} y={313} width={22} height={50} rx={7} fill="url(#tire)" />
      <Rect x={257} y={313} width={22} height={50} rx={7} fill="url(#tire)" />
      <Path d="M126 198h14M126 206h14M126 214h14M126 222h14M126 230h14M260 198h14M260 206h14M260 214h14M260 222h14M260 230h14M124 322h16M124 330h16M124 338h16M124 346h16M124 354h16M260 322h16M260 330h16M260 338h16M260 346h16M260 354h16"
        stroke="rgba(255,255,255,0.06)" strokeWidth={1.5} />

      {/* espejos */}
      <Path d="M142 226 L124 229 Q118 234 124 239 L143 237 Z M258 226 L276 229 Q282 234 276 239 L257 237 Z"
        fill="url(#paint)" stroke="rgba(0,0,0,0.4)" strokeWidth={1} />
      <Path d="M125 231 L138 230 M275 231 L262 230" stroke="#1a1d24" strokeWidth={2.5} strokeLinecap="round" />

      {/* carroceria */}
      <Path d={CUERPO} fill="url(#paint)" />
      <G clipPath="url(#bodyClip)">
        <Rect x={120} y={140} width={160} height={280} fill="url(#shade)" />
        <Path d="M186 140V214M214 140V214M186 352V420M214 352V420" stroke={C.blue} strokeWidth={7} opacity={0.9} />
        <Path d="M186 140V214M214 140V214M186 352V420M214 352V420" stroke={C.blueSoft} strokeWidth={1} opacity={0.5} />
        <Path d="M120 140 H230 Q170 220 120 300 Z" fill="url(#sheen)" />
        <Path d="M262 186 Q270 214 262 242 M138 186 Q130 214 138 242 M263 312 Q272 338 263 366 M137 312 Q128 338 137 366"
          stroke="rgba(0,0,0,0.18)" strokeWidth={2} fill="none" />
        <Path d="M172 160 Q166 185 165 210 M228 160 Q234 185 235 210" stroke="rgba(0,0,0,0.14)" strokeWidth={1.4} fill="none" />
        <Path d="M173 160 Q167 185 166 210 M227 160 Q233 185 234 210" stroke="rgba(255,255,255,0.6)" strokeWidth={0.8} fill="none" />
        <Path d="M150 262 Q148 285 150 306 M250 262 Q252 285 250 306" stroke="rgba(0,0,0,0.18)" strokeWidth={1.2} fill="none" />
        <Path d="M143 300 L150 306 L150 318 Z M257 300 L250 306 L250 318 Z" fill="#14171d" />
      </G>
      <Path d={CUERPO} fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth={1} />

      {/* cabina y techo panoramico */}
      <Path d="M200 208C224 208 238 212 242 222L247 252L247 318C247 340 238 354 200 356C162 354 153 340 153 318L153 252L158 222C162 212 176 208 200 208Z"
        fill="url(#glass)" stroke="rgba(0,0,0,0.5)" strokeWidth={1.2} />
      <Path d="M200 247C222 247 234 248 239 251L240 318C236 324 222 326 200 326C178 326 164 324 160 318L161 251C166 248 178 247 200 247Z"
        fill="url(#roof)" stroke="url(#paint)" strokeWidth={4} />
      <Path d="M166 256 Q200 250 234 256 L232 270 Q200 264 168 270 Z" fill="url(#refl)" />
      <Path d="M170 216 Q200 210 230 216 L233 226 Q200 220 167 226 Z" fill="#fff" opacity={0.12} />
      <Path d="M166 336 Q200 342 234 336" stroke="rgba(255,255,255,0.14)" strokeWidth={1.5} fill="none" />
      <Path d="M175 242 L194 236 M206 242 L225 236" stroke="#0a0b0e" strokeWidth={1.6} strokeLinecap="round" />

      {/* sensores ultrasonicos */}
      <Circle cx={191} cy={155} r={3.2} fill="#14171d" stroke="rgba(255,255,255,0.35)" strokeWidth={0.8} />
      <Circle cx={209} cy={155} r={3.2} fill="#14171d" stroke="rgba(255,255,255,0.35)" strokeWidth={0.8} />

      {/* faros */}
      <Path d="M149 172 Q160 161 181 157 M251 172 Q240 161 219 157" stroke="#14171d" strokeWidth={6} strokeLinecap="round" fill="none" />
      {luces && (
        <G>
          <Path d="M150 171 Q161 161 180 157.5 M250 171 Q239 161 220 157.5" stroke={C.blueSoft} strokeWidth={9} strokeLinecap="round" fill="none" opacity={0.35} />
          <Path d="M150 171 Q161 161 180 157.5 M250 171 Q239 161 220 157.5" stroke="#eef4ff" strokeWidth={2.6} strokeLinecap="round" fill="none" />
        </G>
      )}

      {/* calavera trasera y reversa */}
      <Path d="M146 396 Q200 412 254 396" stroke="#2a0b0c" strokeWidth={6} strokeLinecap="round" fill="none" />
      <Path d="M147 396 Q200 411.5 253 396" stroke={C.r} strokeWidth={8} strokeLinecap="round" fill="none" opacity={0.3} />
      <Path d="M147 396 Q200 411.5 253 396" stroke={C.r} strokeWidth={2.6} strokeLinecap="round" fill="none" />
      {reversa && (
        <G>
          <Path d="M193 405.8 L207 405.8" stroke="#fff" strokeWidth={8} strokeLinecap="round" opacity={0.3} />
          <Path d="M193 405.8 L207 405.8" stroke="#fff" strokeWidth={3} strokeLinecap="round" />
        </G>
      )}
    </Svg>
  );
}

