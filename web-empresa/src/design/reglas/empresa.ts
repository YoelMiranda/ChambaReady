/**
 * Reglas de la web de empresa (funciones puras, probadas con Vitest). Las mismas reglas viven en
 * `:core:domain` (Kotlin), que es lo que usará el backend en F4; aquí se repiten solo para validar
 * y mostrar en el navegador antes de enviar.
 */

// ---------- RUC (E01) ----------

const PREFIJOS_RUC = ["10", "15", "16", "17", "20"];
const PESOS_RUC = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];

/**
 * Valida un RUC peruano: 11 dígitos, prefijo válido y dígito verificador (módulo 11 de SUNAT).
 * Devuelve el mensaje de error o null si es válido.
 */
export function validarRuc(ruc: string): string | null {
  const limpio = ruc.trim();
  if (!/^\d{11}$/.test(limpio)) return "El RUC debe tener 11 dígitos.";
  if (!PREFIJOS_RUC.includes(limpio.slice(0, 2))) return "El RUC debe empezar con 10, 15, 16, 17 o 20.";
  const suma = PESOS_RUC.reduce((total, peso, i) => total + peso * Number(limpio[i]), 0);
  const resto = 11 - (suma % 11);
  const verificador = resto === 10 ? 0 : resto === 11 ? 1 : resto;
  if (verificador !== Number(limpio[10])) return "El RUC no es válido: revisa el último dígito.";
  return null;
}

// ---------- % de ajuste del postulante (E04) ----------

export type TipoSello = "CONFIRMADO" | "DECLARADO" | "NO_CUMPLE";
export type TipoRequisito = "OBLIGATORIO" | "DESEABLE";

export interface RequisitoParaAjuste {
  id: string;
  tipo: TipoRequisito;
  filtro: boolean;
}

/** Peso de cada requisito: filtro > obligatorio > deseable (igual que la probabilidad del estudiante). */
export function pesoRequisito(r: RequisitoParaAjuste): number {
  if (r.filtro) return 3;
  return r.tipo === "OBLIGATORIO" ? 2 : 1;
}

/** Un sello confirmado vale completo; uno declarado vale la mitad porque nadie lo comprobó. */
const PUNTAJE_SELLO: Record<TipoSello, number> = { CONFIRMADO: 1, DECLARADO: 0.5, NO_CUMPLE: 0 };

/**
 * % de ajuste de un postulante a UNA vacante. Sirve solo para ordenar los candidatos de esa vacante:
 * no existe un ranking global y el ranking privado del estudiante nunca se muestra a empresas.
 */
export function calcularAjuste(requisitos: RequisitoParaAjuste[], sellos: Record<string, TipoSello>): number {
  if (requisitos.length === 0) return 100;
  const total = requisitos.reduce((s, r) => s + pesoRequisito(r), 0);
  const obtenido = requisitos.reduce((s, r) => s + pesoRequisito(r) * PUNTAJE_SELLO[sellos[r.id] ?? "DECLARADO"], 0);
  return Math.round((100 * obtenido) / total);
}

// ---------- Precio (E06) ----------

/** Precio por postulante evaluado con el Plan Empresa (precio fijo ÷ postulantes), redondeado al céntimo. */
export function precioPorEvaluadoCentimos(precioMensualCentimos: number, postulantesMes: number): number {
  if (postulantesMes < 0 || !Number.isFinite(postulantesMes)) throw new Error("Postulantes inválidos");
  const divisor = Math.max(1, Math.floor(postulantesMes));
  return Math.floor((precioMensualCentimos + Math.floor(divisor / 2)) / divisor);
}

export function formatoSoles(centimos: number): string {
  return `S/ ${Math.floor(centimos / 100)}.${String(centimos % 100).padStart(2, "0")}`;
}

// ---------- Horarios de entrevista (E07) ----------

export interface EntrevistaAgendada {
  fecha: string; // "AAAA-MM-DD"
  hora: string; // "HH:MM"
  duracionMin: number;
}

export interface Horario {
  hora: string;
  ocupado: boolean;
}

function aMinutos(hora: string): number {
  const [h, m] = hora.split(":").map(Number);
  return h * 60 + m;
}

function aHora(minutos: number): string {
  return `${String(Math.floor(minutos / 60)).padStart(2, "0")}:${String(minutos % 60).padStart(2, "0")}`;
}

/**
 * Horarios de 9:00 a 18:00 cada 30 minutos, sin el almuerzo (13:00–14:00). Un horario está ocupado
 * si una entrevista de esta duración se cruzaría con otra ya agendada ese día o pasaría las 18:00.
 * Si la fecha es hoy, los horarios que ya pasaron también cuentan como ocupados.
 */
export function horariosDelDia(
  fecha: string,
  duracionMin: number,
  agendadas: EntrevistaAgendada[],
  ahora: Date = new Date(),
): Horario[] {
  const inicioDia = 9 * 60;
  const finDia = 18 * 60;
  const delDia = agendadas.filter((e) => e.fecha === fecha).map((e) => [aMinutos(e.hora), aMinutos(e.hora) + e.duracionMin] as const);
  const hoyIso = `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, "0")}-${String(ahora.getDate()).padStart(2, "0")}`;
  const minutoActual = ahora.getHours() * 60 + ahora.getMinutes();
  const horarios: Horario[] = [];
  for (let inicio = inicioDia; inicio + 30 <= finDia; inicio += 30) {
    if (inicio >= 13 * 60 && inicio < 14 * 60) continue;
    const fin = inicio + duracionMin;
    const cruza = delDia.some(([a, b]) => inicio < b && a < fin);
    const pasaAlmuerzo = inicio < 13 * 60 && fin > 13 * 60;
    const yaPaso = fecha === hoyIso && inicio <= minutoActual;
    horarios.push({ hora: aHora(inicio), ocupado: cruza || fin > finDia || pasaAlmuerzo || yaPaso });
  }
  return horarios;
}

/** Máximo de preguntas filtro por vacante (2 preguntas de 45 s). */
export const MAX_PREGUNTAS_FILTRO = 2;

/**
 * Errores de las preguntas filtro que revisó la empresa (F6), o null si están bien. Las mismas
 * reglas que valida el backend: enunciado, 4 opciones distintas, una correcta y un requisito válido.
 */
export function validarPreguntasFiltro(
  preguntas: { requisitoIndice: number; enunciado: string; opciones: string[]; indiceCorrecto: number }[],
  cantidadRequisitos: number,
): string | null {
  if (preguntas.length > MAX_PREGUNTAS_FILTRO) return `Máximo ${MAX_PREGUNTAS_FILTRO} preguntas filtro.`;
  for (const [i, p] of preguntas.entries()) {
    const n = i + 1;
    if (p.requisitoIndice < 0 || p.requisitoIndice >= cantidadRequisitos) return `La pregunta ${n} debe estar ligada a un requisito.`;
    if (p.enunciado.trim().length < 10) return `Escribe el enunciado de la pregunta ${n} (10 caracteres como mínimo).`;
    const opciones = p.opciones.map((o) => o.trim());
    if (opciones.length !== 4 || opciones.some((o) => !o)) return `Completa las 4 opciones de la pregunta ${n}.`;
    if (new Set(opciones.map((o) => o.toLowerCase())).size !== 4) return `Las opciones de la pregunta ${n} deben ser distintas.`;
    if (p.indiceCorrecto < 0 || p.indiceCorrecto > 3) return `Marca la respuesta correcta de la pregunta ${n}.`;
  }
  return null;
}
