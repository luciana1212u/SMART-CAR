# Supabase en SMART-CAR

Las cuentas viven en **Supabase Auth** (`auth.users`) y el perfil en `public.perfiles`
(lo crea el trigger `al_crear_usuario` con los metadatos `nombre` y `usuario`).
El backend no guarda usuarios ni contrasenas: solo llama a Supabase y valida los JWT.
Las apps hablan unicamente con el backend y no llevan claves de Supabase.

## 1. SQL (una vez, idempotente)
Supabase -> SQL Editor -> pegar y ejecutar [`001_auth_smartcar.sql`](001_auth_smartcar.sql).

## 2. Configuracion del backend (secretos fuera de git)
```
cd backend
copy .env.example .env      # y completar los valores; .env esta en .gitignore
```
| Variable | Donde obtenerla |
|---|---|
| `SUPABASE_URL` | Project Settings -> API -> Project URL |
| `SUPABASE_ANON_KEY` | Project Settings -> API -> clave publicable `sb_publishable_...` o la `anon` clasica (nunca `secret` ni `service_role`) |
| `SUPABASE_JWT_SECRET` | Solo si el proyecto firma con HS256 (Project Settings -> JWT Keys -> Legacy). Con claves asimetricas dejar vacio |
| `SMARTCAR_SILENCIO_MS` | Opcional (3000). Sin peticiones de la app durante este tiempo el carro se detiene |
| `SMARTCAR_CORS_ORIGINS` | Opcional (`*`). Origenes web permitidos |

`mvnw spring-boot:run` carga `backend/.env` solo (tambien si arrancas desde la raiz del repo). Las variables de entorno del sistema tienen prioridad sobre `.env`. Si cambias el archivo, reinicia el backend.

## 3. Arranque
```
cd backend && ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
cd frontend/smartcar-app && npm install && npm run web      # web
cd frontend/smartcar-app && npm start                        # movil (Expo Go)
```
En la pantalla de login, "Servidor" es la IP del PC donde corre el backend (puerto 8080).

## API
Publicos: `POST /api/auth/registro`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/usuario-disponible?usuario=`.
Todo lo demas exige `Authorization: Bearer <accessToken>`.
Errores: `USUARIO_DUPLICADO` y `CORREO_DUPLICADO` (409), `CREDENCIALES` y `SIN_SESION`/`SESION_EXPIRADA` (401),
`REGISTRO_FALLIDO` (502), `SUPABASE_NO_DISPONIBLE` y `AUTH_NO_CONFIGURADA` (503).
