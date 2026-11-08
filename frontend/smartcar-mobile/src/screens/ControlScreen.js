import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Platform, Pressable, ScrollView, StatusBar, StyleSheet, Switch, Text, View } from 'react-native';
import { ApiError, crearApi } from '../api';
import { useTema } from '../theme';

const INTERVALO_MS = 500;
const VELOCIDADES = [100, 150, 200, 255];

export default function ControlScreen({ baseUrl, usuario, onSalir }) {
  const t = useTema();
  const api = useMemo(() => crearApi(baseUrl), [baseUrl]);
  const [estado, setEstado] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [velocidad, setVelocidad] = useState(200);
  const cola = useRef(Promise.resolve());     // las ordenes salen en fila: nunca se cruzan
  const consultando = useRef(false);
  const temporizadorAviso = useRef(null);

  const mostrarAviso = useCallback((texto) => {
    setAviso(texto);
    clearTimeout(temporizadorAviso.current);
    temporizadorAviso.current = setTimeout(() => setAviso(null), 4000);
  }, []);

  const manejarError = useCallback((e) => {
    if (e instanceof ApiError && e.status === 401) {
      onSalir('Tu sesion termino. Inicia sesion otra vez.');
      return;
    }
    mostrarAviso(e instanceof ApiError ? e.message : 'Error inesperado');
  }, [mostrarAviso, onSalir]);

  // Refresco periodico del estado
  useEffect(() => {
    let activo = true;
    async function consultar() {
      if (consultando.current) return;
      consultando.current = true;
      try {
        const e = await api.consultarEstado();
        if (activo) setEstado(e);
      } catch (err) {
        if (activo) {
          setEstado(null);
          manejarError(err);
        }
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

  const mover = (cmd) => () => enviar(() => api.enviarMovimiento(cmd));
  const parar = () => enviar(() => api.detener());

  async function salir() {
    try { await api.detener(); } catch { /* la ESP32 tambien se detiene sola al perder sesion */ }
    onSalir(null);
  }

  const hayObstaculo = estado?.hayObstaculo;
  const dist = estado ? (estado.distanciaCm >= 400 ? '—' : Math.round(estado.distanciaCm) + ' cm') : '—';

  return (
    <View style={[s.raiz, { backgroundColor: t.fondo }]}>
      <ScrollView contentContainerStyle={s.contenido}>
        {/* Cabecera */}
        <View style={s.cabecera}>
          <View style={{ flex: 1 }}>
            <Text style={[s.titulo, { color: t.texto }]}>SmartCar</Text>
            <Text style={{ color: t.suave }}>Hola, {usuario?.nombre || usuario?.usuario}</Text>
          </View>
          <Pressable onPress={salir} style={[s.botonChico, { borderColor: t.linea, backgroundColor: t.panel }]}>
            <Text style={{ color: t.texto, fontWeight: '600' }}>Salir</Text>
          </Pressable>
        </View>

        {/* Conexion */}
        <View style={[s.panel, { backgroundColor: t.panel, borderColor: t.linea }]}>
          <View style={s.fila}>
            <View style={[s.punto, { backgroundColor: listo ? t.ok : t.alerta }]} />
            <Text style={[s.estadoTxt, { color: t.texto }]}>
              {!estado ? 'Sin respuesta del servidor'
                : listo ? 'Carro conectado por Bluetooth' : 'Backend sin enlace con el carro'}
            </Text>
          </View>
          {estado && listo && !estado.telemetriaActiva && (
            <Text style={{ color: t.alerta, marginTop: 6 }}>Sin datos recientes del carro</Text>
          )}
          {estado?.bateriaBaja && (
            <Text style={{ color: t.alerta, marginTop: 6 }}>Bateria baja: el carro no se movera hasta cargarlo</Text>
          )}
          {aviso && <Text style={{ color: t.alerta, marginTop: 6, fontWeight: '600' }}>{aviso}</Text>}
        </View>

        {/* Sensores */}
        <View style={s.rejilla}>
          <Dato t={t} titulo="Distancia" valor={dist} alerta={hayObstaculo} />
          <Dato t={t} titulo="Luz" valor={estado ? Math.round(estado.nivelLuz) + ' %' : '—'} />
          <Dato t={t} titulo="Bateria"
            valor={estado ? `${estado.bateriaPct} %` : '—'}
            extra={estado ? estado.voltaje.toFixed(1) + ' V' : null} alerta={estado?.bateriaBaja} />
          <Dato t={t} titulo="Velocidad" valor={estado ? Math.round(estado.velocidadCms) + ' cm/s' : '—'} />
          <Dato t={t} titulo="Estado" valor={estado ? textoEstado(estado.estado) : '—'} />
          <Dato t={t} titulo="Modo" valor={estado ? (auto ? 'Automatico' : 'Manual') : '—'} />
        </View>
        {hayObstaculo && <Text style={{ color: t.alerta, fontWeight: '700', marginBottom: 8 }}>Obstaculo cerca</Text>}

        {/* Movimiento */}
        <Titulo t={t}>Movimiento (manten presionado)</Titulo>
        <View style={s.cruz}>
          <BotonMantener t={t} texto="Avanzar" off={!listo} estilo={s.cAr} onInicio={mover('AVANZAR')} onFin={parar} />
          <BotonMantener t={t} texto="Izquierda" off={!listo} estilo={s.cIz} onInicio={mover('IZQUIERDA')} onFin={parar} />
          <Pressable disabled={!listo} onPress={parar}
            style={[s.cCe, s.botonGrande, { backgroundColor: t.alerta, opacity: listo ? 1 : 0.4 }]}>
            <Text style={[s.botonGrandeTxt, { color: '#fff' }]}>DETENER</Text>
          </Pressable>
          <BotonMantener t={t} texto="Derecha" off={!listo} estilo={s.cDe} onInicio={mover('DERECHA')} onFin={parar} />
          <BotonMantener t={t} texto="Retroceder" off={!listo} estilo={s.cAb} onInicio={mover('RETROCEDER')} onFin={parar} />
        </View>

        {/* Modo y velocidad */}
        <Titulo t={t}>Modo automatico</Titulo>
        <View style={[s.panel, s.fila, { backgroundColor: t.panel, borderColor: t.linea }]}>
          <Text style={{ flex: 1, color: t.texto }}>Esquivar obstaculos solo</Text>
          <Switch value={!!auto} disabled={!listo}
            onValueChange={(v) => enviar(() => api.enviarMovimiento(v ? 'MODO_AUTO' : 'MODO_MANUAL'))} />
        </View>

        <Titulo t={t}>Velocidad</Titulo>
        <View style={s.fila}>
          {VELOCIDADES.map((v) => (
            <Pressable key={v} disabled={!listo}
              onPress={() => { setVelocidad(v); enviar(() => api.enviarMovimiento('VEL:' + v)); }}
              style={[s.opcion, { borderColor: t.linea, opacity: listo ? 1 : 0.4,
                backgroundColor: velocidad === v ? t.acento : t.panel }]}>
              <Text style={{ color: velocidad === v ? t.sobreAcento : t.texto, fontWeight: '600' }}>
                {Math.round(v * 100 / 255)}%
              </Text>
            </Pressable>
          ))}
        </View>

        {/* Accesorios */}
        <Titulo t={t}>Luces</Titulo>
        <View style={s.fila}>
          {[['AUTO', 'Auto'], ['ON', 'Encendidas'], ['OFF', 'Apagadas']].map(([modo, texto]) => (
            <Pressable key={modo} disabled={!listo}
              onPress={() => enviar(() => (modo === 'AUTO' ? api.luzAutomatica() : api.controlarLuces(modo === 'ON')))}
              style={[s.opcion, { borderColor: t.linea, opacity: listo ? 1 : 0.4,
                backgroundColor: estado?.modoLuces === modo ? t.acento : t.panel }]}>
              <Text style={{ color: estado?.modoLuces === modo ? t.sobreAcento : t.texto, fontWeight: '600' }}>{texto}</Text>
            </Pressable>
          ))}
        </View>
        {estado && <Text style={{ color: t.suave, marginTop: 6 }}>
          Luces ahora: {estado.lucesEncendidas ? 'encendidas' : 'apagadas'}</Text>}

        <Titulo t={t}>Bocina (manten presionado)</Titulo>
        <BotonMantener t={t} texto={estado?.bocinaSonando ? 'Sonando...' : 'Bocina'} off={!listo}
          estilo={{ alignSelf: 'stretch' }}
          onInicio={() => enviar(() => api.controlarBocina(true))}
          onFin={() => enviar(() => api.controlarBocina(false))} />

        {/* Eventos */}
        {estado?.eventos?.length > 0 && (
          <>
            <Titulo t={t}>Ultimos eventos</Titulo>
            <View style={[s.panel, { backgroundColor: t.panel, borderColor: t.linea }]}>
              {[...estado.eventos].reverse().slice(0, 6).map((e, i) => (
                <Text key={i} style={{ color: t.suave, fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace', fontSize: 12 }}>{e}</Text>
              ))}
            </View>
          </>
        )}
      </ScrollView>
    </View>
  );
}

function textoEstado(e) {
  return ({ QUIETO: 'Quieto', AVANZANDO: 'Avanzando', RETROCEDIENDO: 'Retrocediendo',
    GIRANDO_IZQUIERDA: 'Girando izq.', GIRANDO_DERECHA: 'Girando der.' })[e] || e;
}

function Titulo({ t, children }) {
  return <Text style={[s.seccion, { color: t.suave }]}>{children}</Text>;
}

function Dato({ t, titulo, valor, extra, alerta }) {
  return (
    <View style={[s.dato, { backgroundColor: t.panel, borderColor: alerta ? t.alerta : t.linea }]}>
      <Text style={{ color: t.suave, fontSize: 12 }}>{titulo}</Text>
      <Text style={{ color: alerta ? t.alerta : t.texto, fontSize: 18, fontWeight: '700' }}>{valor}</Text>
      {extra ? <Text style={{ color: t.suave, fontSize: 12 }}>{extra}</Text> : null}
    </View>
  );
}

// Boton "de hombre muerto": actua al presionar y suelta la orden al levantar el dedo
function BotonMantener({ t, texto, off, estilo, onInicio, onFin }) {
  return (
    <Pressable disabled={off} onPressIn={onInicio} onPressOut={onFin}
      style={({ pressed }) => [s.botonGrande, estilo,
        { backgroundColor: pressed ? t.acento : t.panel, borderColor: t.acento, borderWidth: 2, opacity: off ? 0.4 : 1 }]}>
      {({ pressed }) => (
        <Text style={[s.botonGrandeTxt, { color: pressed ? t.sobreAcento : t.texto }]}>{texto}</Text>
      )}
    </Pressable>
  );
}

const s = StyleSheet.create({
  raiz: { flex: 1, paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 50 },
  contenido: { padding: 16, paddingBottom: 48, maxWidth: 520, width: '100%', alignSelf: 'center' },
  cabecera: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  titulo: { fontSize: 26, fontWeight: '800' },
  botonChico: { borderWidth: 1, borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  panel: { borderWidth: 1, borderRadius: 12, padding: 14, marginBottom: 12 },
  fila: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  punto: { width: 12, height: 12, borderRadius: 6 },
  estadoTxt: { fontWeight: '600', flex: 1 },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 8 },
  dato: { width: '31.5%', flexGrow: 1, borderWidth: 1, borderRadius: 10, padding: 10 },
  seccion: { fontSize: 13, marginTop: 16, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.5 },
  cruz: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  botonGrande: { borderRadius: 12, paddingVertical: 18, alignItems: 'center', justifyContent: 'center', minHeight: 64 },
  botonGrandeTxt: { fontSize: 16, fontWeight: '700' },
  cAr: { width: '32%', marginLeft: '34%' },
  cIz: { width: '32%' },
  cCe: { width: '32%' },
  cDe: { width: '32%' },
  cAb: { width: '32%', marginLeft: '34%' },
  opcion: { flex: 1, borderWidth: 1, borderRadius: 10, paddingVertical: 12, alignItems: 'center' },
});
