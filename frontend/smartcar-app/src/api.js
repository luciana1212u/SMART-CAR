// Cliente HTTP del backend. Toda la app habla SOLO con esto; el Bluetooth lo maneja el backend.
// Cada peticion protegida lleva su JWT (Authorization: Bearer); si caduco se renueva solo con el refresh token.
import { actualizarTokens, borrarSesion, guardarSesion, tokens } from './sesion';

export class ApiError extends Error {
  constructor(status, codigo, mensaje) {
    super(mensaje);
    this.status = status;   // 0 = no hubo respuesta (red / servidor caido)
    this.codigo = codigo;   // SIN_SESION, CARRO_NO_CONECTADO, BATERIA_BAJA, ...
  }
}

// "192.168.1.20" -> "http://192.168.1.20:8080"
export function normalizarUrl(texto) {
  let url = (texto || '').trim();
  if (!url) return '';
  const tieneProtocolo = /^https?:\/\//i.test(url);
  if (!tieneProtocolo) url = 'http://' + url;
  try {
    const destino = new URL(url);
    if (!['http:', 'https:'].includes(destino.protocol) || !destino.hostname || destino.username || destino.password) return '';
    if (!tieneProtocolo && !destino.port) destino.port = '8080';
    return destino.toString().replace(/\/+$/, '');
  } catch {
    return '';
  }
}

// Una sola renovacion a la vez en toda la app: el refresh token es de un solo uso.
let renovando = null;

export function crearApi(baseUrl) {
  async function enviar(metodo, ruta, cuerpo, conToken) {
    const control = new AbortController();
    const temporizador = setTimeout(() => control.abort(), 6000);
    try {
      const headers = { 'Content-Type': 'application/json' };
      const t = conToken ? tokens() : null;
      if (t) headers.Authorization = 'Bearer ' + t.accessToken;
      const resp = await fetch(baseUrl + ruta, {
        method: metodo,
        headers,
        body: cuerpo ? JSON.stringify(cuerpo) : undefined,
        signal: control.signal,
      });
      const texto = await resp.text();
      let datos = null;
      try { datos = texto ? JSON.parse(texto) : null; } catch { /* respuesta no JSON */ }
      if (!resp.ok) {
        throw new ApiError(resp.status, datos?.error, datos?.mensaje || `Error ${resp.status}`);
      }
      return datos;
    } catch (e) {
      if (e instanceof ApiError) throw e;
      throw new ApiError(
        0, 'RED',
        e.name === 'AbortError'
          ? 'El servidor no responde (tiempo agotado)'
          : 'No se pudo conectar. Revisa la URL, que el backend este iniciado y que ambos dispositivos esten en la misma red'
      );
    } finally {
      clearTimeout(temporizador);
    }
  }

  // Devuelve null si se renovo, o el ApiError si no se pudo.
  function renovar() {
    if (!renovando) {
      renovando = (async () => {
        const t = tokens();
        if (!t) return new ApiError(401, 'SIN_SESION', 'Sesion invalida');
        try {
          const r = await enviar('POST', '/api/auth/refresh', { refreshToken: t.refreshToken }, false);
          await actualizarTokens(r.accessToken, r.refreshToken);
          return null;
        } catch (e) {
          return e;
        } finally {
          renovando = null;
        }
      })();
    }
    return renovando;
  }

  async function pedir(metodo, ruta, cuerpo, { publica = false, sinReintento = false } = {}) {
    try {
      return await enviar(metodo, ruta, cuerpo, !publica);
    } catch (e) {
      const caduco = e instanceof ApiError && e.status === 401 && ['SIN_SESION', 'SESION_EXPIRADA'].includes(e.codigo);
      if (publica || sinReintento || !caduco || !tokens()) throw e;
      const fallo = await renovar();
      if (fallo) throw fallo;          // refresh rechazado (401) o sin red: se informa tal cual
      return enviar(metodo, ruta, cuerpo, true);
    }
  }

  return {
    // --- AuthController ---
    registrar: (nombre, usuario, email, password) =>
      pedir('POST', '/api/auth/registro', { nombre, usuario, email, password }, { publica: true }),
    usuarioDisponible: (usuario) =>
      pedir('GET', '/api/auth/usuario-disponible?usuario=' + encodeURIComponent(usuario), null, { publica: true }),
    iniciarSesion: async (email, password) => {
      const r = await pedir('POST', '/api/auth/login', { email, password }, { publica: true });
      await guardarSesion({ url: baseUrl, accessToken: r.accessToken, refreshToken: r.refreshToken, usuario: r.usuario });
      return r;
    },
    cerrarSesion: async () => {
      try { await pedir('POST', '/api/auth/logout', null, { sinReintento: true }); }
      catch { /* sin red o token vencido: igual se borra la sesion local */ }
      await borrarSesion();
    },
    verificarSesion: () => pedir('GET', '/api/auth/sesion'),
    // --- MovimientoController ---
    enviarMovimiento: (comando) => pedir('POST', '/api/movimiento', { comando }),
    detener: () => pedir('POST', '/api/movimiento/detener'),
    // --- EstadoController ---
    consultarEstado: () => pedir('GET', '/api/estado'),
    // --- AccesoriosController ---
    controlarLuces: (encender) => pedir('PUT', '/api/accesorios/luces', { encender }),
    luzAutomatica: () => pedir('POST', '/api/accesorios/luces/auto'),
    controlarBocina: (activar) => pedir('PUT', '/api/accesorios/bocina', { activar }),
  };
}
