/** Tipos que usan las pantallas. Los llena la API simulada (modo demo) o la real (`/v1`). */
import type { TipoRequisito, TipoSello } from "../design/reglas/empresa";

export type { TipoRequisito, TipoSello };

export type EstadoVerificacion = "SIN_VERIFICAR" | "EN_REVISION" | "VERIFICADA";

export interface Empresa {
  razonSocial: string;
  ruc: string;
  sector: string;
  web: string;
  tamano: string;
  contactoNombre: string;
  contactoCorreo: string;
  contactoTelefono: string;
  descripcion: string;
  /** Logo como data URL en el modo demo. Con la API real se usa [logoArchivoId]. */
  logo: string | null;
  verificacion: EstadoVerificacion;
  /** Logo subido al backend (F7): se descarga con GET /v1/archivos/{id}. */
  logoArchivoId?: string | null;
}

export interface SaldoEvaluaciones {
  gratisRestantes: number;
  gratisTotales: number;
  /** True si el Plan Empresa (precio fijo, evaluaciones ilimitadas) está vigente. */
  planActivo: boolean;
  /** Hasta cuándo dura el plan pagado ("AAAA-MM-DD"), o null. */
  planVenceEn?: string | null;
}

/** Un pago SIMULADO (F8): nadie paga de verdad. */
export interface Pago {
  id: string;
  montoCentimos: number;
  fecha: string;
  venceEn: string;
}

export interface EstadoPlan {
  activo: boolean;
  venceEn: string | null;
  precioCentimos: number;
  pagos: Pago[];
}

/** Pregunta filtro de opción múltiple que la empresa revisa antes de publicar (F6). */
export interface PreguntaFiltroNueva {
  /** Posición del requisito en la lista de la vacante. */
  requisitoIndice: number;
  enunciado: string;
  opciones: string[];
  indiceCorrecto: number;
}

export interface PreguntasSugeridas {
  preguntas: PreguntaFiltroNueva[];
  /** false = sin Gemini configurado: son preguntas de ejemplo. */
  conIa: boolean;
}

export interface Notificacion {
  id: string;
  tipo: string;
  titulo: string;
  cuerpo: string;
  vacanteId: string | null;
  leida: boolean;
  fecha: string; // ISO con hora
}

export interface Configuracion {
  /** Precio del Plan Empresa en céntimos, IGV incluido (tabla de configuración). */
  precioPlanCentimos: number;
}

export type NivelExigido = "BASICO" | "INTERMEDIO" | "AVANZADO";
export type Modalidad = "PRESENCIAL" | "HIBRIDA" | "REMOTA";
export type EstadoVacante = "ACTIVA" | "CERRADA";

export interface Requisito {
  id: string;
  habilidad: string;
  nivel: NivelExigido;
  tipo: TipoRequisito;
  filtro: boolean;
}

export interface Vacante {
  id: string;
  puesto: string;
  area: string;
  modalidad: Modalidad;
  sueldo: string | null;
  fechaCierre: string; // "AAAA-MM-DD"
  descripcion: string;
  queHara: string[];
  requisitos: Requisito[];
  evaluacionActiva: boolean;
  creada: string;
  /** Solo al publicar: las preguntas filtro revisadas. Si evalúa y no hay, el backend las genera. */
  preguntasFiltro?: PreguntaFiltroNueva[];
}

/** Vacante con los números que muestra el panel (E02). */
export interface VacanteResumen extends Vacante {
  estado: EstadoVacante;
  diasParaCierre: number;
  postulantes: number;
  evaluados: number;
  confirmados: number;
}

export type EstadoPostulacion = "NUEVA" | "REVISION" | "ENTREVISTA" | "ELEGIDA" | "DESCARTADA";

export interface Evidencia {
  titulo: string;
  estado: "VALIDADA" | "EN_REVISION";
}

export interface Postulante {
  id: string;
  vacanteId: string;
  nombre: string;
  carrera: string;
  universidad: string;
  ciclo: number | null;
  fechaPostulacion: string;
  /** false = entró sin preguntas filtro porque no había saldo ("Sin evaluar"). */
  evaluado: boolean;
  /** Aciertos en las 2 preguntas filtro (solo si fue evaluado). */
  preguntasCorrectas: number | null;
  sellos: Record<string, TipoSello>;
  evidencias: Evidencia[];
  estado: EstadoPostulacion;
  motivoDescarte: string | null;
  /** CV subido por el practicante (F7), o null. */
  cvArchivoId?: string | null;
  /** Regla 13: ya se le pidieron las preguntas y aún no responde. */
  preguntasSolicitadas?: boolean;
}

export type ModalidadEntrevista = "VIDEOLLAMADA" | "PRESENCIAL";

export interface Entrevista {
  id: string;
  postulanteId: string;
  vacanteId: string;
  fecha: string;
  hora: string;
  duracionMin: number;
  modalidad: ModalidadEntrevista;
  mensaje: string;
  /** Nombre del postulante y puesto, para la lista de entrevistas. */
  nombrePostulante?: string;
  puesto?: string;
}

export interface Actividad {
  id: string;
  texto: string;
  fecha: string; // ISO con hora
}

export const ETIQUETA_ESTADO: Record<EstadoPostulacion, string> = {
  NUEVA: "Nueva",
  REVISION: "Revisión",
  ENTREVISTA: "Entrevista",
  ELEGIDA: "Elegida",
  DESCARTADA: "Descartada",
};

export const ETIQUETA_NIVEL: Record<NivelExigido, string> = { BASICO: "Básico", INTERMEDIO: "Intermedio", AVANZADO: "Avanzado" };

export const ETIQUETA_MODALIDAD: Record<Modalidad, string> = { PRESENCIAL: "Presencial", HIBRIDA: "Híbrida", REMOTA: "Remota" };
