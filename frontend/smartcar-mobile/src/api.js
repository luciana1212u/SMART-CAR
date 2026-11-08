// Cliente HTTP del backend. Toda la app habla SOLO con esto; el Bluetooth lo maneja el backend.

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
  if (!/^https?:\/\//i.test(url)) url = 'http://' + url;
  url = url.replace(/\/+$/, '');
  if (!/:\d+$/.test(url)) url += ':8080';
  return url;
}

export function crearApi(baseUrl) {
  async function pedir(metodo, ruta, cuerpo) {
    const control = new AbortController();
    const temporizador = setTimeout(() => control.abort(), 6000);
    try {
      const resp = await fetch(baseUrl + ruta, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
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
          : 'No se pudo conectar. Revisa la IP y que el celular este en la misma WiFi que el PC'
      );
    } finally {
      clearTimeout(temporizador);
    }
  }

  return {
    // --- AuthController ---
    registrar: (nombre, usuario, password) => pedir('POST', '/api/auth/registro', { nombre, usuario, password }),
    iniciarSesion: (usuario, password) => pedir('POST', '/api/auth/login', { usuario, password }),
    cerrarSesion: () => pedir('POST', '/api/auth/logout'),
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
