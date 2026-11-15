// Piezas visuales compartidas por Login y Control (estilo del diseno rc-control).
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { C, txt } from '../theme';

export function T({ style, ...props }) {
  return <Text {...props} style={[txt, { color: C.tx }, style]} />;
}

export function Card({ style, children }) {
  return <View style={[u.card, style]}>{children}</View>;
}

export function CardTitle({ children, style }) {
  return <T style={[u.ctitle, style]}>{children}</T>;
}

// Boton redondeado pequeno. `on` lo pinta de azul (o del color `color`).
export function Pill({ texto, on, off, onPress, onPressIn, onPressOut, color = C.blue, accessibilityLabel, style }) {
  return (
    <Pressable disabled={off} onPress={onPress} onPressIn={onPressIn} onPressOut={onPressOut}
      accessibilityRole="button" accessibilityLabel={accessibilityLabel || texto}
      accessibilityState={{ selected: !!on, disabled: !!off }}
      style={({ hovered, pressed }) => [u.pill, {
        backgroundColor: on ? color : (hovered || pressed) ? C.hover2 : C.hover,
        opacity: off ? 0.4 : 1,
      }, style]}>
      <T style={[u.pillTxt, on && { color: '#fff' }]}>{texto}</T>
    </Pressable>
  );
}

// Boton con borde (estilo "ghost")
export function Ghost({ texto, onPress, off, style }) {
  return (
    <Pressable disabled={off} onPress={onPress} accessibilityRole="button"
      style={({ hovered }) => [u.ghost, hovered && { borderColor: 'rgba(255,255,255,0.4)' }, off && { opacity: 0.4 }, style]}>
      {({ hovered }) => <T style={[u.ghostTxt, hovered && { color: C.tx }]}>{texto}</T>}
    </Pressable>
  );
}

// Boton principal (azul, ancho completo)
export function Primario({ texto, onPress, cargando, children }) {
  return (
    <Pressable onPress={onPress} disabled={cargando} accessibilityRole="button"
      style={({ hovered, pressed }) => [u.primario, {
        backgroundColor: pressed ? '#3359c4' : hovered ? '#4b76ec' : C.blue, opacity: cargando ? 0.7 : 1,
      }]}>
      {children || <T style={u.primarioTxt}>{texto}</T>}
    </Pressable>
  );
}

// Campo de texto con etiqueta; el borde se vuelve azul con el foco
export function Campo({ etiqueta, ayuda, style, ...props }) {
  const [foco, setFoco] = useState(false);
  return (
    <View style={[{ marginTop: 16 }, style]}>
      <T style={u.etiqueta}>{etiqueta}</T>
      <TextInput {...props} onFocus={() => setFoco(true)} onBlur={() => setFoco(false)}
        placeholderTextColor="#5c5e64" accessibilityLabel={props.accessibilityLabel || etiqueta}
        style={[txt, u.campo, foco && { borderColor: C.blue, backgroundColor: '#202024' },
          Platform.OS === 'web' && { outlineStyle: 'none' }]} />
      {ayuda ? <T style={u.ayuda}>{ayuda}</T> : null}
    </View>
  );
}

// Punto de color con brillo
export function Punto({ color, size = 8 }) {
  return <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: color,
    shadowColor: color, shadowOpacity: 0.8, shadowRadius: 6, shadowOffset: { width: 0, height: 0 } }} />;
}

// Icono de bateria (como en el diseno)
export function IconoBateria({ pct, color }) {
  const p = Math.max(0, Math.min(100, pct ?? 0));
  return (
    <View style={u.batt}>
      <View style={{ height: '100%', width: `${p}%`, backgroundColor: color, borderRadius: 1 }} />
      <View style={u.battTip} />
    </View>
  );
}

/**
 * "Hombre muerto": llama `alSoltar` si el dedo/raton se suelta en cualquier parte o la ventana pierde el foco.
 * Devuelve { iniciar, soltar } para usar en onPressIn / onPressOut.
 */
export function useMantener(alIniciar, alSoltar) {
  const presionado = useRef(false);
  const soltar = useCallback(() => {
    if (!presionado.current) return;
    presionado.current = false;
    alSoltar();
  }, [alSoltar]);
  const iniciar = useCallback(() => {
    if (presionado.current) return;
    presionado.current = true;
    alIniciar();
  }, [alIniciar]);

  useEffect(() => {
    if (Platform.OS !== 'web') return undefined;
    const oculto = () => { if (document.hidden) soltar(); };
    window.addEventListener('blur', soltar);
    window.addEventListener('pointerup', soltar);
    window.addEventListener('pointercancel', soltar);
    document.addEventListener('visibilitychange', oculto);
    return () => {
      window.removeEventListener('blur', soltar);
      window.removeEventListener('pointerup', soltar);
      window.removeEventListener('pointercancel', soltar);
      document.removeEventListener('visibilitychange', oculto);
    };
  }, [soltar]);

  return { iniciar, soltar };
}

export const u = StyleSheet.create({
  card: { backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 26, padding: 24 },
  ctitle: { fontSize: 12, fontWeight: '500', letterSpacing: 2.4, textTransform: 'uppercase', color: C.mut },
  pill: { paddingVertical: 8, paddingHorizontal: 14, borderRadius: 999, alignItems: 'center', justifyContent: 'center' },
  pillTxt: { fontSize: 12, fontWeight: '600' },
  ghost: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 999, borderWidth: 1, borderColor: C.line2 },
  ghostTxt: { fontSize: 13, fontWeight: '500', color: C.mut },
  primario: { marginTop: 26, height: 52, borderRadius: 999, alignItems: 'center', justifyContent: 'center' },
  primarioTxt: { fontSize: 15, fontWeight: '600', color: '#fff', letterSpacing: 0.3 },
  etiqueta: { fontSize: 11, fontWeight: '500', letterSpacing: 1.8, textTransform: 'uppercase', color: C.mut, marginBottom: 8 },
  campo: { backgroundColor: C.card2, color: C.tx, borderWidth: 1, borderColor: C.line2, borderRadius: 14,
    paddingHorizontal: 16, paddingVertical: 14, fontSize: 15 },
  ayuda: { fontSize: 12, color: C.mut, marginTop: 6 },
  batt: { width: 26, height: 12, borderWidth: 1.5, borderColor: C.mut, borderRadius: 3, padding: 1.5, justifyContent: 'center' },
  battTip: { position: 'absolute', right: -4.5, top: 2.5, width: 2, height: 4, backgroundColor: C.mut, borderRadius: 1 },
});
