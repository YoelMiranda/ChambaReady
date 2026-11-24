import { auth, urlApi } from "../auth/firebase";

/** Error de la API con su código estable (por ejemplo, EMPRESA_NO_VERIFICADA) y el estado HTTP. */
export class ErrorApi extends Error {
  readonly codigo: string;
  readonly estado: number;

  constructor(codigo: string, mensaje: string, estado: number) {
    super(mensaje);
    this.codigo = codigo;
    this.estado = estado;
  }
}

/**
 * Llama a la API con el ID token de Firebase en `Authorization: Bearer`. Firebase renueva el token
 * solo cuando vence. Los errores de la API se convierten en [ErrorApi] con su mensaje en español.
 */
export async function pedir<T>(metodo: "GET" | "POST" | "PUT" | "PATCH", ruta: string, cuerpo?: unknown): Promise<T> {
  if (!urlApi) throw new Error("Falta VITE_API_BASE_URL");
  const token = await auth?.currentUser?.getIdToken();
  let respuesta: Response;
  try {
    respuesta = await fetch(new URL(ruta.replace(/^\//, ""), urlApi.endsWith("/") ? urlApi : `${urlApi}/`), {
      method: metodo,
      headers: {
        ...(cuerpo !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ErrorApi("SIN_CONEXION", "No pudimos conectarnos. Revisa tu internet e inténtalo otra vez.", 0);
  }
  if (respuesta.status === 204) return undefined as T;
  const datos: unknown = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = datos as { codigo?: string; mensaje?: string } | null;
    throw new ErrorApi(error?.codigo ?? "ERROR", error?.mensaje ?? "Algo salió mal. Inténtalo de nuevo.", respuesta.status);
  }
  return datos as T;
}

/** URL base con "/" al final, para armar las rutas. */
function base(): string {
  if (!urlApi) throw new Error("Falta VITE_API_BASE_URL");
  return urlApi.endsWith("/") ? urlApi : `${urlApi}/`;
}

async function encabezadoToken(): Promise<Record<string, string>> {
  const token = await auth?.currentUser?.getIdToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function errorDe(respuesta: Response): Promise<ErrorApi> {
  const datos = (await respuesta.json().catch(() => null)) as { codigo?: string; mensaje?: string } | null;
  return new ErrorApi(datos?.codigo ?? "ERROR", datos?.mensaje ?? "Algo salió mal. Inténtalo de nuevo.", respuesta.status);
}

/**
 * F7: sube un archivo tal cual (sin multipart) a POST /v1/archivos?tipo=... y devuelve su id.
 * El nombre viaja codificado en el encabezado X-Nombre-Archivo.
 */
export async function subirArchivo(tipo: "LOGO" | "CV" | "EVIDENCIA", archivo: File): Promise<{ id: string; nombre: string }> {
  let respuesta: Response;
  try {
    respuesta = await fetch(new URL(`archivos?tipo=${tipo}`, base()), {
      method: "POST",
      headers: { ...(await encabezadoToken()), "Content-Type": archivo.type || "application/octet-stream", "X-Nombre-Archivo": encodeURIComponent(archivo.name) },
      body: archivo,
    });
  } catch {
    throw new ErrorApi("SIN_CONEXION", "No pudimos conectarnos. Revisa tu internet e inténtalo otra vez.", 0);
  }
  if (!respuesta.ok) throw await errorDe(respuesta);
  return (await respuesta.json()) as { id: string; nombre: string };
}

/**
 * F7: descarga un archivo protegido. Un <img src> no puede mandar el token, por eso se pide con
 * fetch y se muestra con URL.createObjectURL(blob).
 */
export async function descargarArchivo(archivoId: string): Promise<Blob> {
  const respuesta = await fetch(new URL(`archivos/${encodeURIComponent(archivoId)}`, base()), { headers: await encabezadoToken() });
  if (!respuesta.ok) throw await errorDe(respuesta);
  return respuesta.blob();
}
