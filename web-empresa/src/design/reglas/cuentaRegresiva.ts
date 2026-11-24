/**
 * Regla 2 (solo para mostrarla): 3 días o menos en rojo, 4 a 7 en ámbar y más de 7 en verde.
 * La fuente de verdad es `:core:domain` (ReglasCuentaRegresiva), que también usa el backend;
 * la web solo pinta los días que manda la API.
 */
export type Urgencia = "roja" | "ambar" | "verde" | "cerrada";

export function urgencia(diasRestantes: number): Urgencia {
  if (diasRestantes < 0) return "cerrada";
  if (diasRestantes <= 3) return "roja";
  if (diasRestantes <= 7) return "ambar";
  return "verde";
}

export function textoCuentaRegresiva(diasRestantes: number): string {
  if (diasRestantes < 0) return "Cerrada";
  if (diasRestantes === 0) return "Cierra hoy";
  if (diasRestantes === 1) return "Cierra mañana";
  return `Quedan ${diasRestantes} días`;
}
