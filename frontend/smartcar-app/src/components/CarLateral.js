// Vista de PERFIL del carro (de costado, frente hacia la derecha), con el mismo estilo de la vista
// superior: pintura perlada, franja azul, vidrios oscuros. Incluye faro, haz de luz, calavera,
// luz de reversa, ondas de bocina y las zonas de proximidad delante del parachoques.
// Tambien exporta CarroAdaptable: muestra perfil o vista superior y anima el cambio entre ambos.
import React, { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Platform, View } from 'react-native';
import Svg, { Circle, Defs, Ellipse, G, Line, LinearGradient, Path, RadialGradient, Rect, Stop } from 'react-native-svg';
import { C } from '../theme';
import CarView, { cajaCarro } from './CarView';

// ---- Pulsos (mismo comportamiento que en la vista superior) ----
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
  const op = !encendida ? 0.2 : pulso ? 1 - 0.5 * (1 - Math.abs(1 - 2 * f)) : 1;
  return (
    <G opacity={op}>
      {encendida && <Path d={d} fill="none" stroke={color} strokeWidth={22} strokeLinecap="round" opacity={0.18} />}
      <Path d={d} fill="none" stroke={color} strokeWidth={11} strokeLinecap="round" />
    </G>
  );
}

function OndasBocina({ activa }) {
  const f = useOnda(activa, 600);
  if (!activa) return null;
  return <Circle cx={250} cy={112} r={70 * (0.6 + 0.8 * f)} fill="none" stroke={C.y} strokeWidth={2} opacity={0.8 * (1 - f)} />;
}

// Arco de radio r alrededor de (cx, cy) entre los angulos a0..a1 (grados, sentido horario)
function arco(cx, cy, r, a0, a1) {
  const p = (a) => `${(cx + r * Math.cos(a * Math.PI / 180)).toFixed(1)} ${(cy + r * Math.sin(a * Math.PI / 180)).toFixed(1)}`;
  return `M${p(a0)} A${r} ${r} 0 0 1 ${p(a1)}`;
}
const RADIOS = [34, 56, 78];            // rojo (cerca), amarillo, verde (lejos)
const COLORES = [C.r, C.y, C.g];
const ZONA_F = RADIOS.map((r) => arco(452, 118, r, -40, 40));
const ZONA_R = RADIOS.map((r) => arco(40, 118, r, 140, 220));

function Zonas({ paths, nivel }) {
  // nivel 1 = verde, 2 = amarilla, 3 = roja (igual que la vista superior)
  return [2, 1, 0].map((i) => (
    <Banda key={i} d={paths[i]} color={COLORES[i]} encendida={nivel >= 3 - i} pulso={i === 0} />
  ));
}

export function cajaLateral(conAtras) {
  return conAtras ? [-48, 22, 588, 176] : [22, 22, 518, 176];
}

// Coupe deportivo: techo bajo y en caida (fastback), capo largo
const CUERPO = 'M50 140 C42 136 40 118 44 106 C46 98 52 94 62 92 L140 88 C178 72 214 64 252 63 '
  + 'C284 62 304 66 322 76 L352 92 C392 94 420 99 440 106 C450 111 454 122 450 132 C448 138 442 141 432 142 '
  + 'L402 142 A41 41 0 0 0 322 142 L162 142 A41 41 0 0 0 82 142 L60 142 Z';

function Rueda({ cx, cy }) {
  const rayos = [0, 72, 144, 216, 288].map((a) => {
    const r = (a * Math.PI) / 180;
    return <Line key={a} x1={cx + 5 * Math.cos(r)} y1={cy + 5 * Math.sin(r)} x2={cx + 18 * Math.cos(r)} y2={cy + 18 * Math.sin(r)}
      stroke="#5b616b" strokeWidth={4} strokeLinecap="round" />;
  });
  return (
    <G>
      <Circle cx={cx} cy={cy} r={31} fill="#0b0b0d" />
      <Circle cx={cx} cy={cy} r={27} fill="none" stroke="#1f2024" strokeWidth={2} />
      <Circle cx={cx} cy={cy} r={20} fill="url(#rin)" />
      <Path d={arco(cx, cy, 13, 200, 280)} stroke={C.blue} strokeWidth={4} fill="none" strokeLinecap="round" />
      {rayos}
      <Circle cx={cx} cy={cy} r={4.5} fill="#2a2d33" stroke="#9aa0a8" strokeWidth={1} />
    </G>
  );
}

export function CarLateral({ luces = false, reversa = false, bocina = false, nivelFrente = null, nivelAtras = null, style }) {
  const vb = cajaLateral(nivelAtras !== null).join(' ');
  return (
    <Svg viewBox={vb} preserveAspectRatio="xMidYMid meet" style={style}
      accessibilityRole="image" accessibilityLabel="Vista lateral del carro con zonas de proximidad">
      <Defs>
        <LinearGradient id="lpaint" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#ffffff" /><Stop offset="0.3" stopColor="#eef0f3" />
          <Stop offset="0.55" stopColor="#c9cdd3" /><Stop offset="0.8" stopColor="#9da2aa" />
          <Stop offset="1" stopColor="#6f747d" />
        </LinearGradient>
        <LinearGradient id="lglass" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#3a4252" /><Stop offset="0.45" stopColor="#11151c" />
          <Stop offset="1" stopColor="#252c38" />
        </LinearGradient>
        <LinearGradient id="lrefl" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor="#9fb4ff" stopOpacity="0" /><Stop offset="0.5" stopColor="#9fb4ff" stopOpacity="0.3" />
          <Stop offset="1" stopColor="#9fb4ff" stopOpacity="0" />
        </LinearGradient>
        <LinearGradient id="lbeam" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor="#eaf1ff" stopOpacity="0.5" /><Stop offset="1" stopColor="#eaf1ff" stopOpacity="0" />
        </LinearGradient>
        <RadialGradient id="rin" cx="0.4" cy="0.35" r="0.7">
          <Stop offset="0" stopColor="#f2f3f5" /><Stop offset="0.6" stopColor="#b8bcc3" /><Stop offset="1" stopColor="#7b8089" />
        </RadialGradient>
        <RadialGradient id="lsombra" cx="0.5" cy="0.5" r="0.5">
          <Stop offset="0" stopColor="#000" stopOpacity="0.8" /><Stop offset="1" stopColor="#000" stopOpacity="0" />
        </RadialGradient>
        <RadialGradient id="lpiso" cx="0.5" cy="0.5" r="0.5">
          <Stop offset="0" stopColor={C.blue} stopOpacity="0.5" /><Stop offset="1" stopColor={C.blue} stopOpacity="0" />
        </RadialGradient>
      </Defs>

      {/* luz de piso, sombra y haz del faro */}
      {luces && <Ellipse cx={248} cy={176} rx={230} ry={22} fill="url(#lpiso)" />}
      <Ellipse cx={246} cy={184} rx={222} ry={12} fill="url(#lsombra)" />
      {luces && <Path d="M447 112 L540 74 L540 160 Z" fill="url(#lbeam)" />}

      {nivelFrente !== null && <Zonas paths={ZONA_F} nivel={nivelFrente} />}
      {nivelAtras !== null && <Zonas paths={ZONA_R} nivel={nivelAtras} />}
      <OndasBocina activa={bocina} />

      {/* carroceria */}
      <Path d={CUERPO} fill="url(#lpaint)" />
      <Path d="M162 142 L322 142 L319 134 L165 134 Z" fill="rgba(0,0,0,0.22)" />
      {/* franja azul lateral */}
      <Path d="M58 114 C150 110 330 108 446 113" stroke={C.blue} strokeWidth={5} fill="none" opacity={0.9} />
      <Path d="M58 111 C150 107 330 105 446 110" stroke={C.blueSoft} strokeWidth={1} fill="none" opacity={0.6} />
      {/* brillo superior */}
      <Path d="M64 94 L140 90 C178 74 214 66 252 65" stroke="rgba(255,255,255,0.9)" strokeWidth={1.2} fill="none" />
      <Path d="M352 94 C392 96 420 101 438 108" stroke="rgba(255,255,255,0.8)" strokeWidth={1.2} fill="none" />
      {/* vidrios */}
      <Path d="M160 89 C192 74 222 67 252 66 C282 65 300 69 316 78 L338 90 Z" fill="url(#lglass)" stroke="rgba(0,0,0,0.5)" strokeWidth={1} />
      <Path d="M182 84 C208 74 232 70 258 69 L246 78 C224 78 204 81 184 87 Z" fill="url(#lrefl)" />
      <Path d="M212 89 L226 70" stroke="#d5d8dd" strokeWidth={4} strokeLinecap="round" />
      {/* puerta, manija, espejo */}
      <Path d="M216 92 Q212 114 216 136 M334 93 Q338 113 332 136" stroke="rgba(0,0,0,0.22)" strokeWidth={1.4} fill="none" />
      <Rect x={288} y={99} width={20} height={4} rx={2} fill="#7a7f88" />
      <Path d="M334 87 L349 84 Q354 88 349 93 L336 94 Z" fill="url(#lpaint)" stroke="rgba(0,0,0,0.35)" strokeWidth={0.8} />
      {/* toma de aire */}
      <Path d="M150 118 L178 116 L176 124 L152 125 Z" fill="#14171d" />
      {/* contorno */}
      <Path d={CUERPO} fill="none" stroke="rgba(255,255,255,0.5)" strokeWidth={1} />

      {/* faro */}
      <Path d="M430 103 Q444 107 450 117 L440 116 Q435 110 428 107 Z" fill="#14171d" />
      {luces && (
        <G>
          <Path d="M432 105 Q443 109 448 115" stroke={C.blueSoft} strokeWidth={8} strokeLinecap="round" fill="none" opacity={0.35} />
          <Path d="M432 105 Q443 109 448 115" stroke="#eef4ff" strokeWidth={2.6} strokeLinecap="round" fill="none" />
        </G>
      )}
      {/* sensor ultrasonico en el parachoques */}
      <Circle cx={451} cy={125} r={2.6} fill="#14171d" stroke="rgba(255,255,255,0.4)" strokeWidth={0.8} />
      {/* calavera y reversa */}
      <Path d="M45 101 Q47 94 56 92 L57 99 Q50 100 47 105 Z" fill="#2a0b0c" />
      <Path d="M47 100 Q49 95 55 94" stroke={C.r} strokeWidth={7} strokeLinecap="round" fill="none" opacity={0.3} />
      <Path d="M47 100 Q49 95 55 94" stroke={C.r} strokeWidth={2.6} strokeLinecap="round" fill="none" />
      {reversa && (
        <G>
          <Path d="M45 116 L45 124" stroke="#fff" strokeWidth={7} strokeLinecap="round" opacity={0.3} />
          <Path d="M45 116 L45 124" stroke="#fff" strokeWidth={3} strokeLinecap="round" />
        </G>
      )}

      <Rueda cx={122} cy={152} />
      <Rueda cx={362} cy={152} />
    </Svg>
  );
}

// Ultima vista mostrada: sobrevive al cambio de diseno al girar el celular, para animar desde ahi
let ultimaVista = null;

/**
 * Muestra el carro de perfil (lateral) o visto desde arriba (superior) en un ancho dado.
 * Al cambiar de una vista a otra (p. ej. al girar el celular) hace una transicion:
 * la vista vieja se encoge y desaparece mientras la nueva gira y aparece.
 */
export function CarroAdaptable({ vista, ancho, ...props }) {
  const conZonas = props.nivelFrente != null || props.nivelAtras != null;
  const conAtras = props.nivelAtras != null;
  const [, , sW, sH] = cajaCarro(conZonas, conAtras);
  const [, , lW, lH] = cajaLateral(conAtras);
  const lateral = vista === 'lateral';
  const destino = lateral ? 0 : 1;                 // 0 = perfil, 1 = superior
  const inicio = ultimaVista === null ? destino : (ultimaVista === 'lateral' ? 0 : 1);
  const p = useRef(new Animated.Value(inicio)).current;
  const [animando, setAnimando] = useState(inicio !== destino);

  useEffect(() => {
    ultimaVista = vista;
    setAnimando(true);
    const anim = Animated.timing(p, {
      toValue: destino, duration: 700, easing: Easing.out(Easing.back(1.1)),
      useNativeDriver: Platform.OS !== 'web',
    });
    anim.start(({ finished }) => { if (finished) setAnimando(false); });
    return () => anim.stop();
  }, [destino, p, vista]);

  // Espacio reservado segun la vista final
  const caja = lateral ? { width: ancho, height: ancho * lH / lW } : { width: ancho, height: ancho * sH / sW };
  // Cada vista se ajusta dentro de ese espacio
  const ajustar = (w, h) => {
    const k = Math.min(caja.width / w, caja.height / h);
    return { width: w * k, height: h * k };
  };
  const dimL = ajustar(lW, lH);
  const dimS = ajustar(sW, sH);
  const capa = { position: 'absolute', alignItems: 'center', justifyContent: 'center' };

  const verLateral = lateral || animando;
  const verSuperior = !lateral || animando;

  return (
    <View style={[caja, { alignSelf: 'center', alignItems: 'center', justifyContent: 'center' }]}>
      {verLateral && (
        <Animated.View style={[capa, dimL, {
          opacity: p.interpolate({ inputRange: [0, 0.6, 1], outputRange: [1, 0, 0], extrapolate: 'clamp' }),
          transform: [{ scale: p.interpolate({ inputRange: [0, 1], outputRange: [1, 0.7], extrapolate: 'clamp' }) }],
        }]}>
          <CarLateral {...props} style={{ width: '100%', height: '100%' }} />
        </Animated.View>
      )}
      {verSuperior && (
        <Animated.View style={[capa, dimS, {
          opacity: p.interpolate({ inputRange: [0, 0.35, 1], outputRange: [0, 0.2, 1], extrapolate: 'clamp' }),
          transform: [
            { rotate: p.interpolate({ inputRange: [0, 1], outputRange: ['90deg', '0deg'] }) },
            { scale: p.interpolate({ inputRange: [0, 1], outputRange: [0.75, 1] }) },
          ],
        }]}>
          <CarView {...props} style={{ width: '100%', height: '100%' }} />
        </Animated.View>
      )}
    </View>
  );
}
