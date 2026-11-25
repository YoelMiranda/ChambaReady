# Despliegue de la API (Neon + Heroku)

Las cuentas las crea el equipo: nadie más debe tener las claves. Nada de lo que sigue va al repo;
las claves viven en las variables de entorno de Heroku y en tu `backend/.env` local (ignorado por git).

## 1. Base de datos en Neon (plan gratis)

1. Crea una cuenta en <https://neon.com> (puedes entrar con GitHub).
2. **New Project** → nombre `chambaready`, Postgres 17, región **AWS US East (N. Virginia)**: es la más cercana a los servidores de Heroku en EE. UU., así las consultas son más rápidas.
3. En el proyecto, crea la base `chambaready` (o usa `neondb`, la que viene por defecto).
4. **Connect** → marca **Pooled connection** y copia la cadena. Tiene este formato:
   `postgresql://USUARIO:CLAVE@ep-xxxx-pooler.us-east-1.aws.neon.tech/chambaready?sslmode=require`
5. (Recomendado) Crea una rama **`desarrollo`** (Branches → New branch) y usa su cadena en tu computadora. Así las pruebas no tocan los datos de producción.

**Límites del plan gratis:** 1 GB de almacenamiento y 100 CU-horas al mes por proyecto. La base se suspende a los 5 minutos sin uso, así que **la primera solicitud después de un rato tarda unos segundos** en despertarla; la API ya espera hasta 20 segundos por la conexión.

**No hace falta crear tablas:** la API corre las migraciones de Flyway (`backend/src/main/resources/db/migration`) cada vez que arranca.

## 2. Heroku con el GitHub Student Developer Pack

1. Activa el GitHub Student Developer Pack con tu correo de la universidad: <https://education.github.com/pack>.
2. Pide la oferta de Heroku en <https://www.heroku.com/github-students>. Da **US$13 al mes por 24 meses**. **Pide una tarjeta**, y Heroku cobra lo que pase del crédito, así que configura una alerta de gasto en *Account Settings → Billing*.
3. Instala la CLI de Heroku: <https://devcenter.heroku.com/articles/heroku-cli>.
4. Desde la **raíz del repo** (donde están `Dockerfile` y `heroku.yml`):

```bash
heroku login
```
```bash
heroku create chambaready-api
```
```bash
heroku stack:set container -a chambaready-api
```

   Si el nombre `chambaready-api` ya está tomado, elige otro y úsalo en todos los comandos.

5. Configura las variables (reemplaza los valores; **no** pongas `MODO_DESARROLLO` en Heroku):

```bash
heroku config:set -a chambaready-api DATABASE_URL="postgresql://..." ADMIN_TOKEN="$(openssl rand -hex 24)" CORS_ORIGENES="https://TU-WEB.pages.dev,http://localhost:5173"
```

   Para el login (F5) agrega también `FIREBASE_PROJECT_ID` y `FIREBASE_CREDENCIALES_JSON`: los pasos
   están en [firebase.md](firebase.md). Sin ellas, los endpoints protegidos responden 401.

6. Usa un dyno **Basic** (unos US$7 al mes, entra en el crédito). Los dynos Eco se duermen tras 30 minutos sin tráfico.

```bash
heroku ps:type web=basic -a chambaready-api
```

7. **Despliega.** Hay dos opciones:
   - **Con git:** el repo debe ser un repositorio git con un commit. Luego:
     ```bash
     heroku git:remote -a chambaready-api
     ```
     ```bash
     git push heroku main
     ```
   - **Desde GitHub:** Heroku → tu app → *Deploy* → *GitHub* → conecta el repo → *Enable Automatic Deploys* en `main`.

8. Comprueba que responde (debe decir `"estado":"ok"`):

```bash
curl https://chambaready-api.herokuapp.com/v1/salud
```

Para ver los registros: `heroku logs --tail -a chambaready-api`.

## 3. Tareas de administración

Usan el encabezado `X-Admin-Token` con el valor de `ADMIN_TOKEN`. Sin él, esas rutas responden 404.

- **Verificar una empresa** (después de revisar su RUC y su contacto):

```bash
curl -X POST -H "X-Admin-Token: $ADMIN_TOKEN" https://chambaready-api.herokuapp.com/v1/admin/empresas/20131312955/verificar
```

- **Costos de IA (regla 14):** `GET /v1/admin/costos-ia`.
- **Validar una evidencia** (después de revisar el certificado o el link): `POST /v1/admin/evidencias/{id}/validar`.
- **Métricas del proyecto:** `GET /v1/admin/metricas`.

## 3.1 IA (Gemini) — opcional

Sin `GEMINI_API_KEY` todo funciona con **contenido simulado**: preguntas escritas a mano y lectura del
CV solo para archivos Word. Para activar la IA real:

1. Crea una clave en <https://aistudio.google.com/apikey> con la cuenta del equipo.
2. Ponla en `backend/.env` (`GEMINI_API_KEY=...`) y reinicia el backend. Es **secreta**: nunca va a la app ni a la web.
3. Cada llamada queda registrada con sus tokens y su costo en `GET /v1/admin/costos-ia`.

## 4. Correr la API en tu computadora

1. Copia `backend/.env.example` como `backend/.env` y pon la cadena de tu rama `desarrollo` de Neon. Para probar sin login, usa `MODO_DESARROLLO=true` y `CARGAR_DATOS_DEMO=true`. Con las variables de Firebase además acepta tokens reales (primero intenta el token y, si no hay, los encabezados de prueba).
2. En **PowerShell**, dentro de `backend/`: `.\correr.ps1` (carga el `.env` y arranca). O en **Git Bash**, dentro de `backend/`:

```bash
set -a; source .env; set +a; ./gradlew run
```

3. Prueba un endpoint protegido con los encabezados de desarrollo:

```bash
curl -H "X-Demo-Usuario: demo-empresa" -H "X-Demo-Rol: EMPRESA" http://localhost:8080/v1/empresa/vacantes
```

**Pruebas automáticas** (no necesitan Neon: levantan una PostgreSQL embebida):

```bash
./gradlew test
```

## 5. Plan B: Render

Si Heroku falla, en Render: *New → Web Service* → conecta el repo → **Runtime: Docker**, raíz del repo,
*Health Check Path* `/v1/salud` y las mismas variables de entorno. El plan gratis se duerme; el de
US$7 no.
