# Login con Firebase Auth (F5)

Un solo proyecto de Firebase sirve para las tres partes. Firebase solo dice **quién** es la persona;
el **rol** (practicante o empresa) lo guarda el backend en `POST /v1/registro` y no se puede cambiar.

Mientras no completes estos pasos, la app y la web funcionan en **modo demo** (datos de prueba,
sin login), y el backend en tu computadora acepta los encabezados `X-Demo-*` con `MODO_DESARROLLO=true`.

## 1. Crear el proyecto

1. Entra a <https://console.firebase.google.com> con la cuenta del equipo → **Agregar proyecto** → `chambaready`.
   Google Analytics no hace falta.
2. **Compilación → Authentication → Comenzar**. En **Método de acceso** activa solo
   **Correo electrónico/contraseña**. ChambaReady no usa el acceso con Google.
3. En **Authentication → Configuración → Dominios autorizados** agrega el dominio de la web
   (`TU-WEB.pages.dev`). `localhost` ya viene incluido.

## 2. Backend (Heroku y tu computadora)

1. **Configuración del proyecto** (engranaje) → **Cuentas de servicio** → **Generar nueva clave privada**.
   Se descarga un JSON: es **secreto**. No lo subas al repo ni lo mandes por chat.
2. Pásalo a una sola línea y guárdalo en las variables de entorno:
   - `FIREBASE_PROJECT_ID`: el ID del proyecto (por ejemplo `chambaready-1a2b3`).
   - `FIREBASE_CREDENCIALES_JSON`: el contenido del JSON en una línea.

En Git Bash (necesita `jq`; si no lo tienes, copia el texto y quita los saltos de línea a mano):

```bash
heroku config:set -a chambaready-api FIREBASE_PROJECT_ID="chambaready-1a2b3" FIREBASE_CREDENCIALES_JSON="$(jq -c . ruta/al/archivo.json)"
```

Luego borra el JSON descargado o guárdalo en el gestor de contraseñas del equipo.

## 3. Web de empresas

1. **Configuración del proyecto → Tus apps → Agregar app → Web** (`</>`), nombre `web-empresa`.
   No hace falta Firebase Hosting (la web va en Cloudflare Pages).
2. Copia los valores del objeto `firebaseConfig` a `web-empresa/.env.local` (copia de `.env.example`):
   `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_APP_ID`
   y `VITE_API_BASE_URL`.
3. En Cloudflare Pages: **Settings → Environment variables**, las mismas variables.

Estos valores **no son secretos** (viajan en el JavaScript que llega al navegador). Para que nadie
use la clave desde otro sitio, en Google Cloud Console → **APIs y servicios → Credenciales** →
la clave de navegador → **Restricciones de aplicaciones: Sitios web** con tus dominios.

## 4. App Android

1. **Configuración del proyecto → Tus apps → Agregar app → Android**.
   - Nombre del paquete: `pe.edu.esan.chambaready`.
   - El SHA-1 es opcional: con correo y contraseña no hace falta.
2. Descarga `google-services.json` y ponlo en `android/app/`. Está en `.gitignore`: **cada integrante
   lo descarga**. Con ese archivo, Gradle aplica el plugin de Google Services y la app usa el login real.
3. `API_BASE_URL` en `local.properties`: `http://10.0.2.2:8080/v1/` para el backend en tu computadora
   (emulador) o `https://chambaready-api.herokuapp.com/v1/` para Heroku. El HTTP sin cifrar solo se
   permite en builds de depuración y solo hacia `10.0.2.2` y `localhost`.

## Cómo funciona el flujo

```
App / web ──(correo + contraseña)──▶ Firebase Auth ──▶ ID token (vence en 1 h, se renueva solo)
App / web ──(Authorization: Bearer <token>)──▶ API /v1 ──▶ Firebase Admin verifica el token
                                                       └─▶ la base dice el rol (PRACTICANTE / EMPRESA)
```

- **Registro:** la app manda `POST /v1/registro` con `rol=PRACTICANTE`; la web, con `rol=EMPRESA`.
  Si la cuenta ya tenía el otro rol, responde 409 y el cliente cierra la sesión.
- **Iniciar sesión:** el cliente llama a `GET /v1/sesion`. Si la cuenta no está registrada, completa el
  registro; si es del otro rol, se le indica que use la otra plataforma.
- **El token nunca se escribe en los registros:** ni en Logcat (OkHttp lo oculta) ni en los logs del backend.
