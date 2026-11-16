// Inicio de sesion / registro con el estilo del diseno "RC control".
// Pantalla ancha: carro a la izquierda y formulario a la derecha. Celular: carro arriba y formulario abajo.
// La logica (validaciones, Supabase via backend, URL guardada) es la misma de antes.
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StatusBar,
  StyleSheet, View, useWindowDimensions } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ApiError, crearApi, normalizarUrl } from '../api';
import { C } from '../theme';
import CarView from '../components/CarView';
import { CarroAdaptable } from '../components/CarLateral';
import { Campo, Card, CardTitle, Primario, Punto, T } from '../components/ui';

const CLAVE_URL = 'smartcar.servidor';

export default function LoginScreen({ onSesionIniciada, mensajeInicial }) {
  const { width } = useWindowDimensions();
  const amplio = width >= 900;
  const [servidor, setServidor] = useState(() =>
    Platform.OS === 'web' && typeof window !== 'undefined' ? window.location.hostname : '');
  const [modoRegistro, setModoRegistro] = useState(false);
  const [nombre, setNombre] = useState('');
  const [usuario, setUsuario] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [cargando, setCargando] = useState(false);
  const [mensaje, setMensaje] = useState(mensajeInicial ? { texto: mensajeInicial, error: true } : null);

  useEffect(() => {
    AsyncStorage.getItem(CLAVE_URL).then((v) => v && setServidor(v)).catch(() => {});
  }, []);

  async function enviar() {
    if (cargando) return;
    const url = normalizarUrl(servidor);
    if (!url) return setMensaje({ texto: 'Escribe una IP o URL HTTP valida del backend', error: true });
    const correo = email.trim().toLowerCase();
    if (!correo || !password) return setMensaje({ texto: 'Correo y contrasena son obligatorios', error: true });
    const nombreUsuario = usuario.trim().toLowerCase();
    if (modoRegistro) {
      if (!nombre.trim()) return setMensaje({ texto: 'Escribe tu nombre', error: true });
      if (!/^[a-z0-9_]{3,20}$/.test(nombreUsuario)) {
        return setMensaje({ texto: 'El usuario debe tener de 3 a 20 caracteres: letras minusculas, numeros o _', error: true });
      }
      if (password.length < 8) return setMensaje({ texto: 'La contrasena debe tener al menos 8 caracteres', error: true });
    }

    setCargando(true);
    setMensaje(null);
    const api = crearApi(url);
    try {
      if (modoRegistro) {
        await api.registrar(nombre.trim(), nombreUsuario, correo, password);
        setModoRegistro(false);
        setPassword('');
        setMensaje({ texto: 'Cuenta creada. Ahora inicia sesion con tu correo.', error: false });
      } else {
        const r = await api.iniciarSesion(correo, password);
        await AsyncStorage.setItem(CLAVE_URL, url).catch(() => {});
        setPassword('');
        onSesionIniciada(url, r.usuario);
      }
    } catch (e) {
      setMensaje({ texto: e instanceof ApiError ? e.message : 'Error inesperado', error: true });
    } finally {
      setCargando(false);
    }
  }

  const hero = (
    <View style={[s.hero, amplio ? { flex: 1 } : s.heroMovil]}>
      <View style={s.brand}>
        <T style={s.brandB}>SMART·CAR</T>
        <T style={s.brandS}>Control remoto</T>
      </View>
      {amplio && (
        <>
          <T style={s.titulo}>Conduce tu carro{'\n'}desde cualquier pantalla.</T>
          <T style={s.sub}>Control en tiempo real por Bluetooth, sensores de proximidad, luces automáticas y bocina.</T>
        </>
      )}
      {amplio ? (
        <View style={s.carro}>
          <CarView luces style={{ width: '100%', height: '100%' }} />
        </View>
      ) : (
        // Celular: carro de perfil, grande, montado sobre el borde de la tarjeta
        <View style={s.carroMovil}>
          <CarroAdaptable vista="lateral" luces ancho={Math.min(width - 24, 460)} />
        </View>
      )}
      {amplio && (
        <View style={s.rasgos}>
          {[[C.g, 'Zonas de distancia'], [C.blue, 'Modo automático'], [C.y, 'Bocina y luces']].map(([c, t]) => (
            <View key={t} style={s.rasgo}><Punto color={c} /><T style={s.rasgoTxt}>{t}</T></View>
          ))}
        </View>
      )}
    </View>
  );

  const formulario = (
    <Card style={[s.form, amplio ? { width: 440 } : { marginTop: -46, paddingTop: 52 }]}>
      <CardTitle>{modoRegistro ? 'Nueva cuenta' : 'Iniciar sesión'}</CardTitle>
      <T style={s.formTitulo}>{modoRegistro ? 'Crea tu cuenta' : 'Bienvenido de nuevo'}</T>
      <T style={s.formSub}>{modoRegistro ? 'Solo toma un momento.' : 'Entra para controlar el carro.'}</T>

      <Campo etiqueta="Servidor" ayuda="IP del PC o URL del backend" value={servidor} onChangeText={setServidor}
        placeholder="192.168.1.20" editable={!cargando} autoCapitalize="none" autoCorrect={false} keyboardType="url" />

      {modoRegistro && (
        <View style={amplio ? s.dosCampos : undefined}>
          <Campo style={amplio && { flex: 1 }} etiqueta="Nombre" value={nombre} onChangeText={setNombre}
            placeholder="Tu nombre" editable={!cargando} />
          <Campo style={amplio && { flex: 1 }} etiqueta="Usuario" value={usuario} onChangeText={setUsuario}
            placeholder="minúsculas, números o _" autoCapitalize="none" autoCorrect={false}
            maxLength={20} editable={!cargando} />
        </View>
      )}

      <Campo etiqueta="Correo electrónico" value={email} onChangeText={setEmail} placeholder="tu@correo.com"
        autoCapitalize="none" autoCorrect={false} keyboardType="email-address" editable={!cargando} />
      <Campo etiqueta="Contraseña" ayuda={modoRegistro ? 'Mínimo 8 caracteres' : null} value={password}
        onChangeText={setPassword} secureTextEntry autoCapitalize="none" editable={!cargando}
        placeholder="••••••••" onSubmitEditing={enviar} />

      {mensaje && (
        <View style={[s.mensaje, { borderColor: mensaje.error ? 'rgba(255,69,58,0.35)' : 'rgba(48,209,88,0.35)',
          backgroundColor: mensaje.error ? 'rgba(255,69,58,0.10)' : 'rgba(48,209,88,0.10)' }]}>
          <T style={{ color: mensaje.error ? C.r : C.g, fontSize: 14 }}>{mensaje.texto}</T>
        </View>
      )}

      <Primario texto={modoRegistro ? 'Crear cuenta' : 'Entrar'} onPress={enviar} cargando={cargando}>
        {cargando ? <ActivityIndicator color="#fff" /> : null}
      </Primario>

      <Pressable disabled={cargando} onPress={() => { setModoRegistro(!modoRegistro); setMensaje(null); }}
        style={s.enlace} accessibilityRole="button">
        {({ hovered }) => (
          <T style={[s.enlaceTxt, hovered && { color: C.tx }]}>
            {modoRegistro ? '¿Ya tienes cuenta? ' : '¿No tienes cuenta? '}
            <T style={{ color: C.blueSoft, fontWeight: '600' }}>{modoRegistro ? 'Inicia sesión' : 'Crear una'}</T>
          </T>
        )}
      </Pressable>
    </Card>
  );

  return (
    <KeyboardAvoidingView style={s.raiz} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <StatusBar barStyle="light-content" backgroundColor={C.bg} />
      <ScrollView contentContainerStyle={[s.contenido, amplio && s.contenidoAmplio]} keyboardShouldPersistTaps="handled">
        <View style={[s.halo, { pointerEvents: 'none' }]} />
        {hero}
        {formulario}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  raiz: { flex: 1, backgroundColor: C.bg, paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : Platform.OS === 'ios' ? 50 : 0 },
  contenido: { flexGrow: 1, padding: 16, paddingBottom: 40, maxWidth: 520, width: '100%', alignSelf: 'center' },
  contenidoAmplio: { maxWidth: 1180, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 56, padding: 40 },
  halo: { position: 'absolute', top: -200, left: '10%', width: 900, height: 520, borderRadius: 600,
    ...(Platform.OS === 'web'
      ? { backgroundImage: 'radial-gradient(closest-side, rgba(62,106,225,0.16), transparent)' }
      : { backgroundColor: 'rgba(62,106,225,0.06)' }) },

  hero: { maxWidth: 560 },
  heroMovil: { alignItems: 'center', paddingTop: 12, width: '100%', zIndex: 2, elevation: 2 },
  brand: { flexDirection: 'row', alignItems: 'baseline', gap: 14 },
  brandB: { fontWeight: '600', fontSize: 15, letterSpacing: 5 },
  brandS: { fontSize: 13, color: C.mut },
  titulo: { fontSize: 40, lineHeight: 48, fontWeight: '300', letterSpacing: -0.8, marginTop: 36 },
  sub: { fontSize: 15, lineHeight: 23, color: C.mut, marginTop: 14, maxWidth: 440 },
  carro: { height: 340, width: '100%', marginTop: 24 },
  carroMovil: { marginTop: 18, zIndex: 2, elevation: 2 },
  rasgos: { flexDirection: 'row', flexWrap: 'wrap', gap: 22, marginTop: 8 },
  rasgo: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  rasgoTxt: { fontSize: 13, color: C.mut },

  form: { padding: 28, marginTop: 8 },
  formTitulo: { fontSize: 26, fontWeight: '500', marginTop: 14, letterSpacing: -0.3 },
  formSub: { fontSize: 14, color: C.mut, marginTop: 4, marginBottom: 6 },
  dosCampos: { flexDirection: 'row', gap: 12 },
  mensaje: { marginTop: 18, padding: 12, borderRadius: 12, borderWidth: 1 },
  enlace: { alignItems: 'center', paddingTop: 18, paddingBottom: 2 },
  enlaceTxt: { fontSize: 14, color: C.mut },
});
