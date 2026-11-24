import { initializeApp } from "@firebase/app";
import { getAuth, type Auth } from "@firebase/auth";

/**
 * Firebase Authentication de la web de empresa (el mismo proyecto que la app). La configuración es
 * PÚBLICA (Firebase la diseña así): va en variables `VITE_FIREBASE_*`. La seguridad la dan las reglas
 * del backend, que verifica cada token, y las restricciones de la clave en Google Cloud.
 */
const configuracion = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

export const firebaseConfigurado = Object.values(configuracion).every((v) => typeof v === "string" && v.length > 0);

/** null si Firebase no está configurado (la web funciona en modo demo con la API simulada). */
export const auth: Auth | null = firebaseConfigurado ? getAuth(initializeApp(configuracion)) : null;

/** URL de la API (`https://.../v1/`). Sin ella, la web usa la API simulada. */
export const urlApi: string | undefined = import.meta.env.VITE_API_BASE_URL || undefined;

/** True cuando la web habla con el backend real (necesita la API y Firebase). */
export const usaApiReal = Boolean(urlApi) && firebaseConfigurado;
