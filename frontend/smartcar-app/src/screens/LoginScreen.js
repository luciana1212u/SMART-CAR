import React, { useEffect, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StatusBar,
  StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ApiError, crearApi, normalizarUrl } from '../api';
import { useTema } from '../theme';

const CLAVE_URL = 'smartcar.servidor';

export default function LoginScreen({ onSesionIniciada, mensajeInicial }) {
  const t = useTema();
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

  const campo = [s.campo, { backgroundColor: t.panel, borderColor: t.linea, color: t.texto }];

  return (
    <KeyboardAvoidingView style={[s.raiz, { backgroundColor: t.fondo }]} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={s.contenido} keyboardShouldPersistTaps="handled">
        <Text style={[s.titulo, { color: t.texto }]}>SmartCar</Text>
        <Text style={[s.sub, { color: t.suave }]}>
          {modoRegistro ? 'Crea tu cuenta' : 'Inicia sesion para controlar el carro'}
        </Text>

        <Text style={[s.etiqueta, { color: t.suave }]}>Servidor (IP del PC o URL del backend)</Text>
        <TextInput style={campo} value={servidor} onChangeText={setServidor} placeholder="192.168.1.20"
          accessibilityLabel="Servidor" editable={!cargando}
          placeholderTextColor={t.suave} autoCapitalize="none" autoCorrect={false} keyboardType="url" />

        {modoRegistro && (
          <>
            <Text style={[s.etiqueta, { color: t.suave }]}>Nombre</Text>
            <TextInput style={campo} value={nombre} onChangeText={setNombre} placeholder="Tu nombre"
              accessibilityLabel="Nombre" editable={!cargando}
              placeholderTextColor={t.suave} />
          </>
        )}

        {modoRegistro && (
          <>
            <Text style={[s.etiqueta, { color: t.suave }]}>Usuario (unico: minusculas, numeros o _)</Text>
            <TextInput style={campo} value={usuario} onChangeText={setUsuario} autoCapitalize="none"
              accessibilityLabel="Usuario" editable={!cargando} maxLength={20}
              autoCorrect={false} placeholderTextColor={t.suave} />
          </>
        )}

        <Text style={[s.etiqueta, { color: t.suave }]}>Correo electronico</Text>
        <TextInput style={campo} value={email} onChangeText={setEmail} autoCapitalize="none"
          accessibilityLabel="Correo" editable={!cargando} keyboardType="email-address"
          autoCorrect={false} placeholderTextColor={t.suave} />

        <Text style={[s.etiqueta, { color: t.suave }]}>
          {modoRegistro ? 'Contrasena (minimo 8 caracteres)' : 'Contrasena'}
        </Text>
        <TextInput style={campo} value={password} onChangeText={setPassword} secureTextEntry
          accessibilityLabel="Contrasena" editable={!cargando}
          autoCapitalize="none" placeholderTextColor={t.suave} onSubmitEditing={enviar} />

        {mensaje && (
          <Text style={[s.mensaje, { color: mensaje.error ? t.alerta : t.ok }]}>{mensaje.texto}</Text>
        )}

        <Pressable style={[s.boton, { backgroundColor: t.acento, opacity: cargando ? 0.6 : 1 }]}
          onPress={enviar} disabled={cargando}>
          {cargando
            ? <ActivityIndicator color={t.sobreAcento} />
            : <Text style={[s.botonTxt, { color: t.sobreAcento }]}>{modoRegistro ? 'Crear cuenta' : 'Entrar'}</Text>}
        </Pressable>

        <Pressable disabled={cargando} onPress={() => { setModoRegistro(!modoRegistro); setMensaje(null); }} style={s.enlace}>
          <Text style={{ color: t.acento, fontWeight: '600' }}>
            {modoRegistro ? 'Ya tengo cuenta' : 'Crear una cuenta'}
          </Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  raiz: { flex: 1, paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : Platform.OS === 'ios' ? 50 : 0 },
  contenido: { padding: 24, paddingBottom: 48, flexGrow: 1, justifyContent: 'center', maxWidth: 480, width: '100%', alignSelf: 'center' },
  titulo: { fontSize: 32, fontWeight: '800', marginTop: 24 },
  sub: { fontSize: 16, marginBottom: 20 },
  etiqueta: { fontSize: 13, marginTop: 14, marginBottom: 6 },
  campo: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16 },
  mensaje: { marginTop: 14, fontSize: 14 },
  boton: { marginTop: 22, borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  botonTxt: { fontSize: 16, fontWeight: '700' },
  enlace: { alignItems: 'center', padding: 16 },
});
