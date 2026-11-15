[https://drive.google.com/file/d/1m2dbEciEDt3cD1zAqqfnvu2Q6Lm6ApcC/view?usp=sharing](https://drive.google.com/drive/folders/1tg2Um6aZvyMEcis6PEz0ZzQ5PY97z-MO?usp=drive_link)

# Base de datos (Supabase)

La unica base de datos del proyecto es **Supabase** (PostgreSQL + Supabase Auth). El backend Spring Boot
**no guarda usuarios ni contrasenas**: solo llama a la API de Supabase y valida los JWT que esta emite.
Las apps (web y movil) hablan unicamente con el backend y **no llevan ninguna clave de Supabase**.

```
App (Expo)  --->  Backend Spring Boot (:8080)  --->  Supabase (Auth + PostgreSQL)
                  valida el JWT en cada peticion      auth.users + public.perfiles
```

Detalle adicional en [supabase/README.md](supabase/README.md).

## Que se guarda y donde

| Dato | Donde vive | Quien lo escribe |
|---|---|---|
| Correo y contrasena (hash) | `auth.users` (Supabase Auth) | Supabase, al llamar a `/auth/v1/signup` |
| `nombre` y `usuario` | `auth.users.raw_user_meta_data` (se leen como `user_metadata` en el JWT) | El backend, en el campo `data` del registro |
| Perfil publico | `public.perfiles` | El trigger `al_crear_usuario`, al crearse un usuario |
| Sesion | JWT (access token + refresh token) | Supabase; la app los guarda en el dispositivo |

Reglas de los datos:
- **Usuario**: unico sin distinguir mayusculas, 3 a 20 caracteres (la base de datos permite hasta 30), solo minusculas, numeros o `_`.
- **Correo**: unico (lo controla Supabase Auth).
- **Contrasena**: minimo 8 caracteres (el backend lo exige; Supabase tiene su propio minimo, configurado en 6).

Las tablas del carro (motor, sensores, bateria, etc.) **no se guardan en base de datos**: son objetos en memoria del backend.

## Flujo de registro e inicio de sesion

1. `POST /api/auth/registro` -> el backend valida el formato y llama a la funcion `usuario_disponible` (RPC).
2. Si el usuario esta libre, llama a `POST /auth/v1/signup` de Supabase con `email`, `password` y `data: {nombre, usuario}`.
3. Supabase crea la fila en `auth.users` y el trigger `al_crear_usuario` crea la fila en `public.perfiles`
   leyendo `nombre` y `usuario` de `raw_user_meta_data`.
4. `POST /api/auth/login` -> `POST /auth/v1/token?grant_type=password`; devuelve `accessToken` y `refreshToken`.
5. En cada peticion protegida la app envia `Authorization: Bearer <accessToken>`; el backend valida la firma,
   el emisor (`<SUPABASE_URL>/auth/v1`) y la expiracion con `JwtValidator`.
6. `POST /api/auth/refresh` renueva el token; el cierre de sesion usa `/auth/v1/logout?scope=local`.

## Puesta en marcha de la base de datos

### 1. Crear el proyecto en Supabase
Crea un proyecto en <https://supabase.com>. La tabla `public.perfiles` y el trigger `al_crear_usuario` deben existir
(el script del repo **no los crea**: ver "Estructura esperada" mas abajo).

### 2. Ejecutar el SQL del repo (una vez, idempotente)
Supabase -> SQL Editor -> pegar y ejecutar [supabase/001_auth_smartcar.sql](supabase/001_auth_smartcar.sql). Hace dos cosas:
- Crea la funcion `public.usuario_disponible(text)` (devuelve `true/false`, `SECURITY DEFINER`, permisos solo para `anon` y `authenticated`).
- Crea el indice unico `perfiles_usuario_unico` sobre `lower(usuario)` (si hay usuarios repetidos ignorando mayusculas, lo omite y avisa).

No modifica tablas, no toca el trigger, no cambia politicas RLS y no borra datos. Trae consultas de solo lectura al final para verificar.

### 3. Configurar el backend
```
cd backend
copy .env.example .env      # completar los valores; .env esta en .gitignore, no se sube a GitHub
```

| Variable | Donde obtenerla |
|---|---|
| `SUPABASE_URL` | Project Settings -> API -> Project URL (`https://TU-REF.supabase.co`) |
| `SUPABASE_ANON_KEY` | Project Settings -> API Keys -> clave publicable `sb_publishable_...` o la `anon` clasica. **Nunca** la `secret` ni `service_role` |
| `SUPABASE_JWT_SECRET` | Solo si el proyecto firma con HS256 (JWT Keys -> Legacy). Con claves asimetricas (ES256/RS256) dejar vacio: se leen del JWKS del proyecto |
| `SMARTCAR_SILENCIO_MS` | Opcional (3000). Sin peticiones de la app durante este tiempo el carro se detiene |
| `SMARTCAR_CORS_ORIGINS` | Opcional (`*`). Origenes web permitidos |

El archivo `.env` se carga solo con `mvnw spring-boot:run`. Las variables de entorno del sistema tienen prioridad sobre `.env`.
**Si cambias el `.env`, reinicia el backend.**

### 4. Configurar Supabase Auth
- **Confirmacion de correo**: si esta activa, el usuario queda "Waiting for verification" y no puede iniciar sesion hasta confirmar (el backend responde `CORREO_SIN_CONFIRMAR`, 403).
  Para desarrollo conviene desactivarla (Authentication -> Providers -> Email -> "Confirm email"); si tu panel no la muestra, crea los usuarios de prueba con "Auto Confirm User" o configura un SMTP propio.
- **Limite de correos**: el servidor de correo por defecto de Supabase permite muy pocos envios por hora.
  Cada registro con confirmacion activa envia un correo, y reintentar con un correo "Waiting for verification" tambien. Ver "Problemas frecuentes".
- **Longitud minima de contrasena**: Authentication -> Providers -> Email. El backend ya exige 8.

## Estructura esperada en Supabase

El backend asume (sin crearlo el script del repo) lo siguiente:

- Tabla `public.perfiles` con las columnas `id`, `nombre` y `usuario`, y estas restricciones (verificadas en Supabase):

| Restriccion | Definicion |
|---|---|
| `perfiles_pkey` | `PRIMARY KEY (id)` |
| `perfiles_id_fkey` | `FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE` |
| `perfiles_usuario_key` | `UNIQUE (usuario)` |
| `usuario_formato` | `CHECK (usuario ~ '^[a-z0-9_]{3,30}$')` |
| `nombre_no_vacio` | `CHECK (length(trim(both from nombre)) > 0)` |

  El backend es mas estricto que la base de datos: acepta usuarios de 3 a 20 caracteres, mientras que el `CHECK` permite hasta 30.
  Como `id` tiene `ON DELETE CASCADE`, al borrar un usuario en Authentication -> Users su perfil se borra solo.
- Politica RLS que solo deja ver el perfil propio (por eso la comprobacion de disponibilidad usa una funcion `SECURITY DEFINER`).
- Trigger `al_crear_usuario`, definido asi en Supabase:

```sql
create or replace function public.crear_perfil_usuario()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.perfiles (id, nombre, usuario)
    values (
        new.id,
        trim(new.raw_user_meta_data ->> 'nombre'),
        lower(trim(new.raw_user_meta_data ->> 'usuario'))
    );
    return new;
end;
$$;

create trigger al_crear_usuario
after insert on auth.users
for each row
execute function public.crear_perfil_usuario();
```

El trigger corre despues de cada alta en `auth.users`. Guarda el `usuario` en minusculas y sin espacios.
Si el insert falla (por ejemplo, `usuario` repetido), Supabase cancela todo el registro y responde con un error de base de datos.
El backend lo traduce a `USUARIO_DUPLICADO` o a `REGISTRO_FALLIDO`.

Comprobaciones de solo lectura (SQL Editor):
```sql
select proname, prosecdef, proconfig from pg_proc where proname = 'usuario_disponible';
select indexname from pg_indexes where indexname = 'perfiles_usuario_unico';
select tgname from pg_trigger where tgname = 'al_crear_usuario' and not tgisinternal;
select trigger_name, event_object_table, action_timing, event_manipulation
  from information_schema.triggers where trigger_name = 'al_crear_usuario';
select conname, pg_get_constraintdef(oid) from pg_constraint where conrelid = 'public.perfiles'::regclass;
select public.usuario_disponible('un_usuario_que_no_existe');   -- true = libre
```

## Endpoints relacionados

Publicos: `POST /api/auth/registro`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/usuario-disponible?usuario=`.
Todo lo demas exige `Authorization: Bearer <accessToken>`.

| Codigo | HTTP | Significado |
|---|---|---|
| `USUARIO_DUPLICADO` / `CORREO_DUPLICADO` | 409 | El usuario o correo ya existe |
| `CREDENCIALES` | 401 | Correo o contrasena incorrectos |
| `SIN_SESION` / `SESION_EXPIRADA` | 401 | Falta el token o ya expiro |
| `CORREO_SIN_CONFIRMAR` | 403 | La cuenta no confirmo el correo |
| `DEMASIADOS_INTENTOS` | 429 | Limite de Supabase (ver abajo) |
| `REGISTRO_FALLIDO` | 502 | Supabase no pudo crear la cuenta (por ejemplo, fallo el trigger) |
| `SUPABASE_NO_DISPONIBLE` / `AUTH_NO_CONFIGURADA` | 503 | Sin conexion o faltan `SUPABASE_URL` / `SUPABASE_ANON_KEY` |

## Problemas frecuentes

**"Demasiados registros seguidos. Espera un momento e intenta de nuevo"**
No lo genera el backend: es el 429 de Supabase. Para ver la causa exacta, busca en la consola del backend la linea
`Supabase rechazo el registro: estado=... codigo=... mensaje=...`.
- `over_email_send_rate_limit`: Supabase intento enviar un correo y se agoto el cupo del servidor por defecto.
  Pasa con "Confirm email" activo, o al reintentar un correo que quedo "Waiting for verification".
  Solucion: borrar ese usuario en Authentication -> Users, desactivar la confirmacion (o usar SMTP propio), esperar ~1 hora a que se libere el cupo.
- `over_request_rate_limit` / `over_signup_rate_limit`: limite por IP. Esperar unos minutos o subirlo en Authentication -> Rate Limits.

**Error de usuario duplicado aunque borraste la cuenta**
Borrar el usuario en Authentication -> Users elimina tambien su perfil (`ON DELETE CASCADE`). Si sigue saliendo, el nombre de usuario lo tiene otra cuenta:
`select usuario, id from public.perfiles where usuario = 'el_usuario';`

**`REGISTRO_FALLIDO` con "error al guardar el perfil"**
Fallo el trigger `al_crear_usuario` (nombre de usuario repetido, o el trigger/tabla no existen). Revisa el trigger y la tabla `perfiles`.

**`SUPABASE_NO_DISPONIBLE` al comprobar el usuario**
Falta ejecutar `supabase/001_auth_smartcar.sql` o no hay conexion con Supabase.

**El backend ignora los cambios del `.env`**
Reinicialo. Las variables de entorno del sistema (`$env:SUPABASE_URL`) tienen prioridad sobre el archivo.

## Pruebas

Los tests no usan Supabase real: [FakeSupabase.java](backend/src/test/java/com/smartcar/backend/FakeSupabase.java) simula `/auth/v1/*` y `/rest/v1/rpc/usuario_disponible`.
```
cd backend && ./mvnw test
```
