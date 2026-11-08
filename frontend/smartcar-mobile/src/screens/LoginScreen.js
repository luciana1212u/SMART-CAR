import React, { useEffect, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StatusBar,
  StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ApiError, crearApi, normalizarUrl } from '../api';
import { useTema } from '../theme';

const CLAVE_URL = 'smartcar.servidor';

export default function LoginScreen({ onSesionIniciada }) {
  const t = useTema();
  const [servidor, setServidor] = useState('');
  const [modoRegistro, setModoRegistro] = useState(false);
  const [nombre, setNombre] = useState('');
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [cargando, setCargando] = useState(false);
  const [mensaje, setMensaje] = useState(null);   // { texto, error }

  useEffect(() => {
    AsyncStorage.getItem(CLAVE_URL).then((v) => v && setServidor(v)).catch(() => {});
  }, []);

  async function enviar() {
    const url = normalizarUrl(servidor);
    if (!url) return setMensaje({ texto: 'Escribe la IP del PC donde corre el backend', error: true });
    if (!usuario.trim() || !password) return setMensaje({ texto: 'Usuario y contrasena son obligatorios', error: true });

    setCargando(true);
    setMensaje(null);
    const api = crearApi(url);
    try {
      if (modoRegistro) {
        await api.registrar(nombre.trim(), usuario.trim(), password);
        setModoRegistro(false);
        setMensaje({ texto: 'Cuenta creada. Ahora inicia sesion.', error: false });
      } else {
        const r = await api.iniciarSesion(usuario.trim(), password);
        await AsyncStorage.setItem(CLAVE_URL, url).catch(() => {});
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

        <Text style={[s.etiqueta, { color: t.suave }]}>Servidor (IP del PC)</Text>
        <TextInput style={campo} value={servidor} onChangeText={setServidor} placeholder="192.168.1.20"
          placeholderTextColor={t.suave} autoCapitalize="none" autoCorrect={false} keyboardType="url" />

        {modoRegistro && (
          <>
            <Text style={[s.etiqueta, { color: t.suave }]}>Nombre</Text>
            <TextInput style={campo} value={nombre} onChangeText={setNombre} placeholder="Tu nombre"
              placeholderTextColor={t.suave} />
          </>
        )}

        <Text style={[s.etiqueta, { color: t.suave }]}>Usuario</Text>
        <TextInput style={campo} value={usuario} onChangeText={setUsuario} autoCapitalize="none"
          autoCorrect={false} placeholderTextColor={t.suave} />

        <Text style={[s.etiqueta, { color: t.suave }]}>Contrasena</Text>
        <TextInput style={campo} value={password} onChangeText={setPassword} secureTextEntry
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

        <Pressable onPress={() => { setModoRegistro(!modoRegistro); setMensaje(null); }} style={s.enlace}>
          <Text style={{ color: t.acento, fontWeight: '600' }}>
            {modoRegistro ? 'Ya tengo cuenta' : 'Crear una cuenta'}
          </Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  raiz: { flex: 1, paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 50 },
  contenido: { padding: 24, maxWidth: 480, width: '100%', alignSelf: 'center' },
  titulo: { fontSize: 32, fontWeight: '800', marginTop: 24 },
  sub: { fontSize: 16, marginBottom: 20 },
  etiqueta: { fontSize: 13, marginTop: 14, marginBottom: 6 },
  campo: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16 },
  mensaje: { marginTop: 14, fontSize: 14 },
  boton: { marginTop: 22, borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  botonTxt: { fontSize: 16, fontWeight: '700' },
  enlace: { alignItems: 'center', padding: 16 },
});
