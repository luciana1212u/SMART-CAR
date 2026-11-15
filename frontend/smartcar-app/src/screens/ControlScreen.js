// Pantalla de control con el diseno "RC control": control circular, carro con zonas de proximidad
// y panel de informacion. En pantallas anchas usa dos columnas; en el celular, una sola.
// Toda la logica (cola de ordenes, refresco cada 500 ms, sesion) es la misma de antes.
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Platform, Pressable, ScrollView, StatusBar, StyleSheet, View, useWindowDimensions } from 'react-native';
import Svg, { Circle, Path, Rect } from 'react-native-svg';
import { ApiError, crearApi } from '../api';
import { C } from '../theme';
import CarView, { cajaCarro } from '../components/CarView';
import DPad from '../components/DPad';
import { Card, CardTitle, Ghost, IconoBateria, Pill, Punto, T, useMantener } from '../components/ui';

const INTERVALO_MS = 500;
const NIVELES = [80, 124, 168, 211, 255];          // PWM para cada nivel de potencia (VEL:n acepta 80-255)
const COMANDOS = { UP: 'AVANZAR', DOWN: 'RETROCEDER', LEFT: 'IZQUIERDA', RIGHT: 'DERECHA' };
const NOMBRES = { UP: 'Adelante', DOWN: 'Reversa', LEFT: 'Izquierda', RIGHT: 'Derecha', STOP: 'Detenido' };
const ESTADOS = { QUIETO: 'Detenido', AVANZANDO: 'Adelante', RETROCEDIENDO: 'Reversa',
  GIRANDO_IZQUIERDA: 'Izquierda', GIRANDO_DERECHA: 'Derecha' };
const ZONA = ['Libre', 'Verde', 'Amarilla', 'Roja'];
const ZONA_COLOR = [C.mut, C.g, C.y, C.r];
const TECLAS = { ArrowUp: 'UP', w: 'UP', ArrowDown: 'DOWN', s: 'DOWN', ArrowLeft: 'LEFT', a: 'LEFT',
  ArrowRight: 'RIGHT', d: 'RIGHT', ' ': 'STOP' };
const SENSOR_TRASERO = false;   // cambiar a true si se instala el sensor trasero

const nivelZona = (cm) => (cm == null || cm >= 400 ? 0 : cm < 30 ? 3 : cm < 70 ? 2 : cm < 120 ? 1 : 0);
const colorBateria = (p) => (p < 20 ? C.r : p < 40 ? C.y : C.g);

export default function ControlScreen({ baseUrl, usuario, onSalir }) {
  const { width, height } = useWindowDimensions();
  const amplio = width >= 1000;
  const compacto = width < 420;
  const api = useMemo(() => crearApi(baseUrl), [baseUrl]);
  const [estado, setEstado] = useState(null);
  const [latencia, setLatencia] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [nivel, setNivel] = useState(4);
  const [dir, setDir] = useState(null);           // direccion que el usuario mantiene presionada
  const dirRef = useRef(null);
  const cola = useRef(Promise.resolve());         // las ordenes salen en fila: nunca se cruzan
  const consultando = useRef(false);
  const temporizadorAviso = useRef(null);

  const mostrarAviso = useCallback((texto) => {
    setAviso(texto);
    clearTimeout(temporizadorAviso.current);
    temporizadorAviso.current = setTimeout(() => setAviso(null), 4000);
  }, []);

  const manejarError = useCallback((e) => {
    if (e instanceof ApiError && e.status === 401) {
      onSalir('Tu sesión terminó. Inicia sesión otra vez.');
      return;
    }
    mostrarAviso(e instanceof ApiError ? e.message : 'Error inesperado');
  }, [mostrarAviso, onSalir]);

  // Refresco periodico del estado (y medicion de latencia)
  useEffect(() => {
    let activo = true;
    async function consultar() {
      if (consultando.current) return;
      consultando.current = true;
      const t0 = Date.now();
      try {
        const e = await api.consultarEstado();
        if (activo) { setEstado(e); setLatencia(Date.now() - t0); }
      } catch (err) {
        if (activo) { setEstado(null); setLatencia(null); manejarError(err); }
      } finally {
        consultando.current = false;
      }
    }
    consultar();
    const id = setInterval(consultar, INTERVALO_MS);
    return () => { activo = false; clearInterval(id); clearTimeout(temporizadorAviso.current); };
  }, [api, manejarError]);

  const enviar = useCallback((accion) => {
    cola.current = cola.current.then(accion).catch(manejarError);
  }, [manejarError]);

  const listo = !!estado?.esp32Conectada;
  const auto = estado?.modo === 'AUTO';

  // Al conectar el carro se le envia la potencia elegida, para que app y carro coincidan
  const nivelRef = useRef(nivel);
  nivelRef.current = nivel;
  useEffect(() => {
    if (listo) enviar(() => api.enviarMovimiento('VEL:' + NIVELES[nivelRef.current - 1]));
  }, [listo, api, enviar]);

  function elegirNivel(n) {
    setNivel(n);
    enviar(() => api.enviarMovimiento('VEL:' + NIVELES[n - 1]));
  }

  // Cambia la direccion mantenida. null = se solto (si se estaba moviendo, se detiene).
  const cambiarDir = useCallback((d) => {
    const previa = dirRef.current;
    if (d === previa) return;
    dirRef.current = d;
    setDir(d);
    if (COMANDOS[d]) enviar(() => api.enviarMovimiento(COMANDOS[d]));
    else if (d === 'STOP' || (d === null && previa && previa !== 'STOP')) enviar(() => api.detener());
  }, [api, enviar]);

  // Si se desconecta el carro mientras se mantiene una flecha, se libera
  useEffect(() => { if (!listo && dirRef.current) { dirRef.current = null; setDir(null); } }, [listo]);

  // Bocina (mantener) y luces
  const bocina = useMantener(
    useCallback(() => enviar(() => api.controlarBocina(true)), [api, enviar]),
    useCallback(() => enviar(() => api.controlarBocina(false)), [api, enviar]));
  const lucesEn = !!estado?.lucesEncendidas;
  const alternarLuces = useCallback(() => enviar(() => api.controlarLuces(!lucesEn)), [api, enviar, lucesEn]);
  const alternarModo = () => enviar(() => api.enviarMovimiento(auto ? 'MODO_MANUAL' : 'MODO_AUTO'));

  // Teclado en web: WASD / flechas, Espacio detiene, H bocina, L luces
  const teclado = useRef({});
  teclado.current = { listo, cambiarDir, bocina, alternarLuces };
  useEffect(() => {
    if (Platform.OS !== 'web') return undefined;
    const escribiendo = (e) => /INPUT|TEXTAREA/.test(e.target?.tagName || '');
    const abajo = (e) => {
      const k = teclado.current;
      if (e.repeat || escribiendo(e) || !k.listo) return;
      const tecla = e.key.length === 1 ? e.key.toLowerCase() : e.key;
      if (tecla === 'h') { k.bocina.iniciar(); return; }
      if (tecla === 'l') { k.alternarLuces(); return; }
      const d = TECLAS[tecla];
      if (!d) return;
      e.preventDefault();
      k.cambiarDir(d);
    };
    const arriba = (e) => {
      const k = teclado.current;
      const tecla = e.key.length === 1 ? e.key.toLowerCase() : e.key;
      if (tecla === 'h') { k.bocina.soltar(); return; }
      const d = TECLAS[tecla];
      if (d && d === dirRef.current) k.cambiarDir(null);
    };
    const perderFoco = () => { if (dirRef.current) teclado.current.cambiarDir(null); };
    window.addEventListener('keydown', abajo);
    window.addEventListener('keyup', arriba);
    window.addEventListener('blur', perderFoco);
    return () => {
      window.removeEventListener('keydown', abajo);
      window.removeEventListener('keyup', arriba);
      window.removeEventListener('blur', perderFoco);
    };
  }, []);

  async function salir() {
    try { await api.detener(); } catch { /* la ESP32 tambien se detiene sola al perder sesion */ }
    onSalir(null);
  }

  // ---- Datos derivados ----
  const telemetria = listo && !!estado?.telemetriaActiva;
  const distancia = estado && estado.distanciaCm < 400 ? Math.round(estado.distanciaCm) : null;
  const zonaF = telemetria ? nivelZona(distancia) : 0;
  const bateriaReal = estado && estado.bateriaPct >= 0;
  const bateria = bateriaReal ? estado.bateriaPct : null;   // el carro no tiene medidor: B:-1
  const luz = estado && telemetria ? Math.round(estado.nivelLuz) : null;
  const textoDir = dir && dir !== 'STOP' ? NOMBRES[dir] : estado ? (ESTADOS[estado.estado] || estado.estado) : 'Detenido';
  const reversa = dir === 'DOWN' || estado?.estado === 'RETROCEDIENDO';

  const conexion = !estado ? { color: C.r, texto: 'Sin servidor' }
    : !listo ? { color: C.r, texto: 'Carro desconectado' }
      : !estado.telemetriaActiva ? { color: C.y, texto: 'Sin datos' }
        : { color: C.g, texto: 'Conectado' };

  const avisos = [
    aviso,
    estado?.bateriaBaja && 'Batería baja: el carro no se moverá hasta cargarlo',
    estado && !listo && 'El backend no tiene enlace Bluetooth con el carro',
  ].filter(Boolean);

  // ---- Bloques ----
  const cabecera = (
    <View style={[s.top, compacto && { paddingHorizontal: 16 }]}>
      <View style={s.brand}>
        <T style={s.brandB}>SMART·CAR</T>
        <T style={s.brandS}>Hola, {usuario?.nombre || usuario?.usuario}</T>
      </View>
      <View style={[s.status, compacto && { gap: 14 }]}>
        <View style={s.statusItem}><Punto color={conexion.color} /><T style={s.statusB}>{conexion.texto}</T></View>
        {latencia !== null && <T style={s.statusTxt}>{latencia} ms</T>}
        {bateria !== null && (
          <View style={s.statusItem}>
            <IconoBateria pct={bateria} color={colorBateria(bateria)} />
            <T style={s.statusB}>{bateria}%</T>
          </View>
        )}
        <Ghost texto="Salir" onPress={salir} />
      </View>
    </View>
  );

  // ---- Medidas segun el tipo de pantalla ----
  const conAtras = SENSOR_TRASERO;
  const [, , vbW, vbH] = cajaCarro(true, conAtras);
  const pad = compacto ? 12 : 16;                       // margen lateral en el celular
  const tamDpad = amplio ? 300 : Math.min(250, width - 2 * pad - 40);

  // Carro visto desde arriba, con las zonas de proximidad
  const carro = (ancho) => (
    <CarView style={{ width: ancho, height: ancho * vbH / vbW, alignSelf: 'center' }}
      luces={lucesEn} reversa={reversa} bocina={!!estado?.bocinaSonando}
      nivelFrente={zonaF} nivelAtras={conAtras ? 0 : null} />
  );

  const leyenda = (corta) => (
    <View style={[s.legend, corta && { columnGap: 16, marginTop: 4 }]}>
      {[[C.g, 'Verde 70–120 cm', '70–120'], [C.y, 'Amarilla 30–70 cm', '30–70'], [C.r, 'Roja menos de 30 cm', '< 30 cm']].map(([c, t, tc]) => (
        <View key={t} style={s.legendItem}><Punto color={c} size={corta ? 7 : 10} /><T style={[s.legendTxt, corta && { fontSize: 11, color: C.mut }]}>{corta ? tc : t}</T></View>
      ))}
    </View>
  );

  const tituloControl = (
    <View style={s.fila}>
      <CardTitle>Control</CardTitle>
      <View style={[s.fila, { gap: 10 }]}>
        <View style={[s.modo, auto && { backgroundColor: 'rgba(62,106,225,0.18)', borderColor: C.blue }]}>
          <T style={[s.modoTxt, auto && { color: C.blueSoft }]}>{auto ? 'AUTOMÁTICO' : 'MANUAL'}</T>
        </View>
      </View>
    </View>
  );

  const segmentos = (
    <View style={s.segs}>
      {NIVELES.map((_, i) => (
        <Pressable key={i} disabled={!listo} onPress={() => elegirNivel(i + 1)}
          accessibilityRole="button" accessibilityLabel={'Potencia ' + (i + 1)}
          style={[s.seg, { opacity: listo ? 1 : 0.4 }]}>
          {({ hovered }) => <View style={[s.segBar, { backgroundColor: i < nivel ? C.tx : hovered ? C.hover2 : C.hover }]} />}
        </Pressable>
      ))}
    </View>
  );

  const filasControl = (
    <>
      <View style={s.row}><T style={s.rowLab}>Dirección</T><T style={s.rowVal}>{textoDir}</T></View>
      <View style={[s.row, { marginTop: 18, paddingTop: 18 }]}>
        <T style={s.rowLab}>Potencia</T>
        <T style={s.rowVal}>Nivel {nivel} de 5 <T style={s.rowLab}>· {Math.round(NIVELES[nivel - 1] * 100 / 255)}%</T></T>
      </View>
      {segmentos}
      <T style={s.hint}>
        {Platform.OS === 'web'
          ? 'Mantén presionada una flecha para avanzar. Teclado: W A S D o flechas, Espacio detiene, H bocina, L luces.'
          : 'Mantén presionada una flecha para avanzar. Puedes deslizar el dedo para cambiar de dirección.'}
      </T>
    </>
  );

  const dpad = <DPad size={tamDpad} activo={dir} off={!listo} onDir={cambiarDir} />;

  const info = (
    <Card style={s.info}>
      <CardTitle>Información</CardTitle>
      <View style={{ marginTop: 10 }}>
        <Fila primera icono={<IconoAuto />} titulo="Modo automático"
          valor={auto ? 'Activo' : 'Manual'} colorValor={auto ? C.blueSoft : undefined}>
          <Pill texto={auto ? 'Desactivar' : 'Activar'} on={auto} off={!listo} onPress={alternarModo} />
        </Fila>
        <Fila icono={<IconoBocina />} titulo="Bocina"
          valor={estado?.bocinaSonando ? 'Sonando' : 'Inactiva'} colorValor={estado?.bocinaSonando ? C.y : undefined}>
          <Pill texto="Sonar" on={!!estado?.bocinaSonando} off={!listo} color={C.y}
            accessibilityLabel="Sonar bocina (mantener)" onPressIn={bocina.iniciar} onPressOut={bocina.soltar} />
        </Fila>
        <Fila icono={<IconoLed />} titulo="Luces"
          valor={compacto ? null : lucesEn ? 'Encendidas' : 'Apagadas'} colorValor={lucesEn ? C.g : undefined}>
          <View style={s.pills}>
            <Pill texto="Auto" on={estado?.modoLuces === 'AUTO'} off={!listo} onPress={() => enviar(() => api.luzAutomatica())} />
            <Pill texto={lucesEn ? 'Apagar' : 'Encender'} on={estado?.modoLuces === 'ON'} off={!listo} onPress={alternarLuces} />
          </View>
        </Fila>
        <Barra icono={<IconoSol />} titulo="Sensor de luz" valor={luz === null ? '—' : luz + '%'} pct={luz ?? 0}
          colores={['#5a5f6a', C.y]} />
        <Barra icono={<IconoBateriaChica />} titulo="Batería"
          valor={bateria === null ? 'Sin medidor' : bateria + '%'} pct={bateria ?? 0}
          colores={[colorBateria(bateria ?? 0), colorBateria(bateria ?? 0)]} />
        <Fila icono={<IconoMotor />} titulo="Motor (PWM)" valor={telemetria ? estado.pwmPct + '%' : '—'} />
      </View>
    </Card>
  );

  const eventos = estado?.eventos?.length > 0 && (
    <Card style={s.eventos}>
      <CardTitle>Últimos eventos</CardTitle>
      <View style={{ marginTop: 12, gap: 4 }}>
        {[...estado.eventos].reverse().slice(0, 6).map((e, i) => (
          <T key={i} style={[s.evento, i === 0 && { color: C.tx }]}>{e}</T>
        ))}
      </View>
    </Card>
  );

  const lectura = (corta) => <Lectura etiqueta="FRENTE" cm={telemetria ? distancia : null} nivel={zonaF} corta={corta} />;

  let cuerpo;
  if (amplio) {
    // Computador: control + informacion a la izquierda; carro y eventos a la derecha.
    // El carro se dimensiona para que su caja quepa en la ventana (sin hueco debajo).
    const anchoCol = width - 64 - 400 - 20;
    const altoCarro = Math.max(360, height - 110 - 196);
    const anchoCarro = Math.min(altoCarro * vbW / vbH, anchoCol - 56);
    cuerpo = (
      <View style={s.main}>
        <View style={s.col}>
          <Card style={s.ctrl}>{tituloControl}<View style={s.dpad}>{dpad}</View>{filasControl}</Card>
          {info}
        </View>
        <View style={[{ flex: 1, gap: 20 }, Platform.OS === 'web' && { position: 'sticky', top: 16 }]}>
          <Card style={s.stage}>
            <View style={s.fila}>
              <CardTitle>Sensores de proximidad</CardTitle>
              {estado?.hayObstaculo && <T style={s.obstaculo}>Obstáculo</T>}
            </View>
            {lectura(false)}
            <View style={{ marginVertical: 8 }}>{carro(anchoCarro)}</View>
            {leyenda(false)}
          </Card>
          {eventos}
        </View>
      </View>
    );
  } else {
    // Celular / pantalla angosta: una sola columna
    const anchoCarro = Math.min(width - 2 * pad - 40, 300);
    cuerpo = (
      <View style={[s.mainMovil, { paddingHorizontal: pad }]}>
        <Card style={[s.stage, { padding: 20 }]}>
          <View style={s.fila}>
            <CardTitle>Sensores de proximidad</CardTitle>
            {estado?.hayObstaculo && <T style={s.obstaculo}>Obstáculo</T>}
          </View>
          {lectura(false)}
          <View style={{ marginVertical: 8 }}>{carro(anchoCarro)}</View>
          {leyenda(true)}
        </Card>
        <Card style={[s.ctrl, { padding: 20 }]}>{tituloControl}<View style={s.dpad}>{dpad}</View>{filasControl}</Card>
        {info}
        {eventos}
      </View>
    );
  }

  return (
    <View style={s.raiz}>
      <StatusBar barStyle="light-content" backgroundColor={C.bg} />
      <ScrollView contentContainerStyle={s.contenido}>
        <View style={[s.halo, { pointerEvents: 'none' }]} />
        {cabecera}
        {avisos.length > 0 && (
          <View style={[s.avisos, !amplio && { marginHorizontal: pad }]}>
            {avisos.map((a) => <T key={a} style={s.avisoTxt}>{a}</T>)}
          </View>
        )}
        {cuerpo}
      </ScrollView>
    </View>
  );
}

// ---- Subcomponentes ----

function Lectura({ etiqueta, cm, nivel, corta }) {
  const color = ZONA_COLOR[nivel];
  if (corta) {
    return (
      <View style={[s.fila, { justifyContent: 'center', gap: 10 }]}>
        <T style={[s.roLab, { width: undefined, fontSize: 11 }]}>{etiqueta}</T>
        <T style={{ fontSize: 26, fontWeight: '300' }}>{cm === null ? '—' : cm}<T style={{ fontSize: 13, color: C.mut }}> cm</T></T>
        <View style={[s.roTag, { minWidth: 0 }]}><Punto color={color} size={8} /><T style={[s.roTagTxt, { color, fontSize: 12 }]}>{ZONA[nivel]}</T></View>
      </View>
    );
  }
  return (
    <View style={s.ro}>
      <T style={s.roLab}>{etiqueta}</T>
      <T style={s.roNum}>{cm === null ? '—' : cm}</T>
      <T style={s.roUnit}>cm</T>
      <View style={s.roTag}><Punto color={color} size={9} /><T style={[s.roTagTxt, { color }]}>{ZONA[nivel]}</T></View>
    </View>
  );
}

function Fila({ icono, titulo, valor, colorValor, primera, children }) {
  return (
    <View style={[s.irow, primera && s.irowPrimera]}>
      <View style={s.ilab}><View style={s.icono}>{icono}</View><T style={s.ilabTxt}>{titulo}</T></View>
      <View style={s.ival}>
        {valor != null && <T style={[s.ivalTxt, colorValor && { color: colorValor }]}>{valor}</T>}
        {children}
      </View>
    </View>
  );
}

function Barra({ icono, titulo, valor, pct, colores, extra, nota }) {
  return (
    <View style={[s.irow, { flexDirection: 'column', alignItems: 'stretch' }]}>
      <View style={[s.fila, { marginBottom: 10 }]}>
        <View style={s.ilab}><View style={s.icono}>{icono}</View><T style={s.ilabTxt}>{titulo}</T></View>
        <View style={s.ival}><T style={s.ivalTxt}>{valor}</T>{extra}</View>
      </View>
      <View style={s.lbar}>
        <View style={{ height: '100%', width: `${Math.max(0, Math.min(100, pct))}%`, borderRadius: 3,
          backgroundColor: colores[1], ...(Platform.OS === 'web'
            ? { backgroundImage: `linear-gradient(90deg, ${colores[0]}, ${colores[1]})` } : {}) }} />
      </View>
      {nota ? <T style={s.nota}>{nota}</T> : null}
    </View>
  );
}

// Iconos de linea (mismos trazos del diseno)
const ico = { width: 20, height: 20, viewBox: '0 0 24 24' };
const trazo = { fill: 'none', stroke: C.icon, strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' };
function IconoBocina() {
  return <Svg {...ico}><Path {...trazo} d="M4 10v4h4l5 4V6L8 10H4z" /><Path {...trazo} d="M16.5 9a4 4 0 0 1 0 6M19 6.5a7.5 7.5 0 0 1 0 11" /></Svg>;
}
function IconoLed() {
  return <Svg {...ico}><Path {...trazo} d="M9 18h6M10 21h4" /><Path {...trazo} d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" /></Svg>;
}
function IconoSol() {
  return <Svg {...ico}><Circle {...trazo} cx={12} cy={12} r={4} /><Path {...trazo} d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></Svg>;
}
function IconoAuto() {
  return <Svg {...ico}><Path {...trazo} d="M12 3l2.2 4.6 5 .7-3.6 3.5.9 5L12 14.4 7.5 16.8l.9-5L4.8 8.3l5-.7z" /></Svg>;
}
function IconoBateriaChica() {
  return <Svg {...ico}><Rect {...trazo} x={3} y={7} width={16} height={10} rx={2} /><Path {...trazo} d="M22 11v2M7 10v4M11 10v4" /></Svg>;
}
function IconoMotor() {
  return <Svg {...ico}><Circle {...trazo} cx={12} cy={12} r={8} /><Path {...trazo} d="M12 12l4-3M12 4v2M4 12h2M18 12h2" /></Svg>;
}

const s = StyleSheet.create({
  raiz: { flex: 1, backgroundColor: C.bg, paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : Platform.OS === 'ios' ? 50 : 0 },
  contenido: { paddingBottom: 32 },
  halo: { position: 'absolute', top: -260, right: '-10%', width: 1100, height: 520, borderRadius: 600,
    backgroundColor: 'rgba(62,106,225,0.10)',
    ...(Platform.OS === 'web' ? { backgroundColor: 'transparent',
      backgroundImage: 'radial-gradient(closest-side, rgba(62,106,225,0.14), transparent)' } : { opacity: 0.6 }) },

  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 14,
    paddingHorizontal: 32, paddingTop: 22, paddingBottom: 14 },
  brand: { flexDirection: 'row', alignItems: 'baseline', gap: 14, flexWrap: 'wrap' },
  brandB: { fontWeight: '600', fontSize: 15, letterSpacing: 5 },
  brandS: { fontSize: 13, color: C.mut },
  status: { flexDirection: 'row', alignItems: 'center', gap: 22, flexWrap: 'wrap' },
  statusItem: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  statusB: { fontSize: 14, fontWeight: '500' },
  statusTxt: { fontSize: 14, color: C.mut },

  avisos: { marginHorizontal: 32, marginBottom: 10, padding: 14, borderRadius: 16, gap: 4,
    backgroundColor: 'rgba(255,69,58,0.10)', borderWidth: 1, borderColor: 'rgba(255,69,58,0.35)' },
  avisoTxt: { color: C.r, fontSize: 14, fontWeight: '500' },

  main: { flexDirection: 'row', gap: 20, paddingHorizontal: 32, paddingTop: 8, alignItems: 'flex-start' },
  mainMovil: { gap: 16, paddingHorizontal: 16, paddingTop: 4 },
  col: { width: 400, gap: 20 },

  fila: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  ctrl: { padding: 28 },
  modo: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 999, borderWidth: 1, borderColor: C.line2 },
  modoTxt: { fontSize: 11, fontWeight: '600', letterSpacing: 1.5, color: C.mut },
  dpad: { alignItems: 'center', marginTop: 26, marginBottom: 4 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 24, paddingTop: 20,
    borderTopWidth: 1, borderTopColor: C.line },
  rowLab: { fontSize: 14, color: C.mut, fontWeight: '400' },
  rowVal: { fontSize: 17, fontWeight: '500' },
  segs: { flexDirection: 'row', gap: 8, marginTop: 4 },
  seg: { flex: 1, paddingVertical: 10 },
  segBar: { height: 10, borderRadius: 8 },
  hint: { marginTop: 18, fontSize: 13, lineHeight: 20, color: C.mut },

  info: { paddingHorizontal: 28, paddingVertical: 24 },
  irow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12, paddingVertical: 14,
    borderTopWidth: 1, borderTopColor: C.line },
  irowPrimera: { borderTopWidth: 0, paddingTop: 4 },
  ilab: { flexDirection: 'row', alignItems: 'center', gap: 12, flexShrink: 1 },
  icono: { width: 20, height: 20, flexShrink: 0 },
  ilabTxt: { fontSize: 14, color: C.mut },
  ival: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  ivalTxt: { fontSize: 15, fontWeight: '500', textAlign: 'right' },
  pills: { flexDirection: 'row', gap: 6 },
  lbar: { height: 6, borderRadius: 3, backgroundColor: C.hover, overflow: 'hidden' },
  nota: { fontSize: 12, color: C.mut, marginTop: 8 },

  stage: { padding: 28, minHeight: 0 },
  obstaculo: { fontSize: 12, fontWeight: '600', letterSpacing: 1.2, color: C.r, textTransform: 'uppercase' },
  ro: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'center', gap: 12, marginTop: 14 },
  roLab: { fontSize: 12, fontWeight: '500', letterSpacing: 2.6, color: C.mut, width: 64, textAlign: 'right' },
  roNum: { fontSize: 46, fontWeight: '300', lineHeight: 50, letterSpacing: -1 },
  roUnit: { fontSize: 16, color: C.mut, marginLeft: -6 },
  roTag: { flexDirection: 'row', alignItems: 'center', gap: 8, minWidth: 100, alignSelf: 'center' },
  roTagTxt: { fontSize: 13, fontWeight: '600', letterSpacing: 0.8 },
  legend: { flexDirection: 'row', justifyContent: 'center', flexWrap: 'wrap', columnGap: 26, rowGap: 8, marginTop: 6 },
  legendItem: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  legendTxt: { fontSize: 13 },

  eventos: { paddingHorizontal: 28, paddingVertical: 22 },
  evento: { fontSize: 12, color: C.mut, fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace' },
});
