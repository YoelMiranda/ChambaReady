# ChambaReady

Plataforma con dos productos que comparten una misma API:

1. **App Android para estudiantes** (`android/`): diagnostica qué les falta para una vacante y arma un plan que cabe en su tiempo libre.
2. **Web para empresas** (`web-empresa/`): publican vacantes, ven candidatos con sellos por requisito, agendan entrevistas y pagan por evaluar.

| Carpeta | Qué es | Estado |
|---|---|---|
| `android/` | Kotlin + Compose + Material 3, MVVM, Hilt | F9: todo el flujo conectado a la API (sin compilar: falta abrirla en Android Studio) |
| `web-empresa/` | React + Vite + TypeScript, React Router, TanStack Query | F9: preguntas filtro con IA, logo, CV de postulantes, pago simulado y notificaciones |
| `backend/` | API Ktor `/v1` + PostgreSQL (Neon) + Flyway | F9: IA (Gemini o simulada), archivos en la base, pagos simulados, notificaciones y métricas |
| `docs/` | Fórmula, cobro, despliegue, API y [Firebase](docs/firebase.md) | En curso |

## Cómo correr cada parte

### App Android

1. Instala Android Studio (estable, compatible con AGP 9.5) y la plataforma **Android 16 (API 36)** desde el SDK Manager.
2. **File > Open** y elige la carpeta `android/` (no la raíz del repo).
3. Copia `android/local.properties.example` como `android/local.properties`.
4. Ejecuta `app` en un emulador con API 26 o superior.

**Modo demo o real:** sin `android/app/google-services.json` la app corre en **modo demo** (sin login,
datos de prueba en el teléfono). Con ese archivo usa Firebase Auth (correo y contraseña) y la API de
`API_BASE_URL`. Los pasos están en [docs/firebase.md](docs/firebase.md).

Desde la terminal, dentro de `android/`:

```bash
./gradlew :app:assembleDebug
./gradlew test
```

Pruebas instrumentadas de navegación (necesitan un emulador abierto):

```bash
./gradlew :app:connectedDebugAndroidTest
```

**Recorrer la app:** en la Bienvenida usa «Explorar con datos de prueba» (solo en builds de
depuración) para entrar con un perfil ficticio, o «Soy practicante» para hacer el registro completo.
Todo lo que cambias (horas, área, Premium, tareas, evidencias, notificaciones leídas) se guarda en
DataStore. Con login, el perfil también se sube solo al backend cada vez que cambia
(`SincronizadorPerfil`), y «¿Ya tienes cuenta? Inicia sesión» lo recupera en otro teléfono.
«Cerrar sesión» en Perfil cierra la sesión y borra los datos del teléfono.

### Web de empresa

Necesitas Node 22.22 o superior. Dentro de `web-empresa/`:

```bash
npm install
npm run dev
```

Se abre en <http://localhost:5173>. Otros comandos: `npm run build`, `npm run typecheck` y `npm test`.

Sin `web-empresa/.env.local` corre en **modo demo** (datos en el navegador, sin login). Con la
configuración web de Firebase y `VITE_API_BASE_URL` (ver `.env.example` y
[docs/firebase.md](docs/firebase.md)) pide iniciar sesión y usa la API real.

### Backend

Ktor 3 + Exposed + Flyway sobre PostgreSQL (Neon). Reutiliza las reglas de `android/core/domain`,
así que la app y la API calculan lo mismo. Necesitas JDK 17. Dentro de `backend/`:

```bash
./gradlew test
```

Las pruebas levantan una PostgreSQL embebida: no necesitan Neon ni internet después de la primera
descarga. Para correr la API, desplegarla en Heroku y crear las cuentas, sigue
[docs/despliegue.md](docs/despliegue.md). Los endpoints están en [docs/api.md](docs/api.md).

## Módulos Android

```
android/
├── app/                  UI: pantallas, ViewModels, navegación (Navigation Compose), Hilt
├── core/domain/          Kotlin PURO: modelos y reglas de negocio + pruebas (sin Android)
├── core/data/            DataStore, contenido de prueba, Retrofit + Firebase Auth (remoto/, auth/)
└── core/designsystem/    ChambaReadyTheme y 12 componentes con @Preview
```

`:core:domain` no depende de Android: el backend lo reutiliza para aplicar las mismas reglas, y
permite migrar a Compose Multiplatform si se hace la versión para iPhone.

### Reglas de negocio en `:core:domain`

| Regla | Archivo |
|---|---|
| Horas libres por día (0–8 h, pasos de 0.5) | `modelo/HorasLibres.kt` |
| Viabilidad (horas por día real hasta el cierre) | `reglas/ReglasViabilidad.kt` |
| Cuenta regresiva (rojo ≤ 3, ámbar 4–7, verde > 7) | `reglas/ReglasCuentaRegresiva.kt` |
| Zonas y fórmula de probabilidad | `reglas/ZonaProbabilidad.kt`, `reglas/ReglasProbabilidad.kt` |
| Postular y sellos | `reglas/ReglasPostulacion.kt` |
| Nivel demostrado y mini-test (2 de 3) | `reglas/ReglasNivel.kt` |
| Saldo de evaluaciones, precio por evaluado y margen | `empresa/Evaluaciones.kt`, `empresa/CalculadoraPrecioEmpresa.kt` |

## Navegación

- **Android:** "atrás" siempre hace `popBackStack()`, así que vuelve a la pantalla anterior real. La barra inferior guarda y restaura el estado de cada pestaña (`irAPestana` en `ChambaReadyNavHost.kt`).
- **Web:** rutas reales con React Router. El detalle del candidato vive en `/candidatos/:vacanteId/:postulanteId`.

## Convenciones (heredadas de SportPro)

- Android: `XxxScreen` (conecta el ViewModel) + `XxxContenido` (sin estado, con `@Preview`), y `XxxViewModel : BaseViewModel<XxxUiState, XxxEvent>`.
- Nombres de dominio en español, sufijos técnicos en inglés y comentarios en español.
- Colores y medidas solo desde el sistema de diseño: `ChambaTema`/`Dimensiones` en Android y `tokens.css` en la web.

## Fases

| Fase | Qué se hizo |
|---|---|
| F1–F5 | Diseño, pantallas, API, base de datos y login con Firebase |
| F6 | IA solo desde el backend: lee el CV, genera mini-tests y preguntas filtro (con contenido simulado si no hay clave). Ranking privado en el servidor |
| F7 | Archivos (CV, evidencias, logo) **guardados en la base** en vez de Cloudflare R2, con permisos y validación de firma |
| F8 | **Pagos simulados**: nadie paga; activa el plan o Premium por 30 días con historial |
| F9 | Notificaciones dentro de la app y la web (sin push) y métricas para el equipo |
| F10 | Omitida: no se despliega. Todo corre en la PC (backend en el puerto 8081 si el 8080 está ocupado) |

## Secretos

Nunca se suben: `local.properties`, `google-services.json`, `.env*` (salvo `.env.example`) ni archivos de claves.
La app y la web no tienen la clave de Gemini: todo pasa por la API. No hay pasarela de pago ni R2.
En la web, todo lo que empieza con `VITE_` llega al navegador; ahí solo van valores públicos.
