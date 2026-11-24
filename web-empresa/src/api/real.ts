/**
 * API real (backend /v1). Convierte los DTO del backend a los tipos que usan las pantallas, para que
 * las pantallas sean las mismas con la API simulada o con la real. Ver docs/api.md.
 */
import type { ApiEmpresa } from "./contrato";
import { descargarArchivo, ErrorApi, pedir, subirArchivo } from "./http";
import type {
  Actividad, Configuracion, Empresa, Entrevista, EstadoPlan, EstadoPostulacion, Modalidad, NivelExigido, Notificacion, Postulante,
  PreguntasSugeridas, Requisito, SaldoEvaluaciones, TipoSello, Vacante, VacanteResumen,
} from "./tipos";

// ---------- DTO del backend (mismos nombres de campo que backend/.../dto/Dtos.kt) ----------

interface RequisitoApi { id: string; habilidadId: string; habilidad: string; nivel: string; tipo: string }

interface VacanteEmpresaApi {
  id: string; puesto: string; area: string; modalidad: string; sueldo: string | null; descripcion: string;
  queHara: string[]; creada: string; fechaCierre: string; diasParaCierre: number; estado: string;
  evaluacionActiva: boolean; requisitos: RequisitoApi[]; postulantes: number; evaluados: number; confirmados: number;
}

interface PostulanteApi {
  id: string; vacanteId: string; nombre: string; carrera: string; universidad: string; ciclo: number | null; fecha: string;
  evaluado: boolean; preguntasCorrectas: number | null; sellos: Record<string, string>; estado: string; motivoDescarte: string | null; ajuste: number;
  cvArchivoId?: string | null; preguntasSolicitadas?: boolean;
}

interface EmpresaApi {
  razonSocial: string; ruc: string; sector: string; web: string; tamano: string; contactoNombre: string;
  contactoCorreo: string; contactoTelefono: string; descripcion: string; verificacion: string; logoArchivoId: string | null;
}

interface EntrevistaApi {
  id: string; postulacionId: string; vacanteId: string; nombrePostulante: string; puesto: string;
  fecha: string; hora: string; duracionMin: number; modalidad: string; mensaje: string;
}

// ---------- Conversiones (funciones puras, probadas en real.test.ts) ----------

/** Código de área del backend → nombre que se muestra (el mismo catálogo que /v1/catalogo/areas). */
export const AREAS: Record<string, string> = {
  DATA_ANALYTICS: "Data y Analytics",
  DESARROLLO_SOFTWARE: "Desarrollo de software",
  MARKETING_DIGITAL: "Marketing digital",
  FINANZAS: "Finanzas",
  OPERACIONES_LOGISTICA: "Operaciones y logística",
  DISENO_UX_UI: "Diseño UX/UI",
  RECURSOS_HUMANOS: "Recursos humanos",
  VENTAS_COMERCIAL: "Ventas y comercial",
};

export function areaACodigo(nombre: string): string {
  return Object.entries(AREAS).find(([, n]) => n === nombre)?.[0] ?? nombre;
}

/** El backend guarda un requisito filtro como tipo FILTRO; la web lo muestra como obligatorio + filtro. */
export function aRequisitoWeb(r: RequisitoApi): Requisito {
  return {
    id: r.id,
    habilidad: r.habilidad,
    nivel: r.nivel as NivelExigido,
    tipo: r.tipo === "DESEABLE" ? "DESEABLE" : "OBLIGATORIO",
    filtro: r.tipo === "FILTRO",
  };
}

export function aVacanteWeb(v: VacanteEmpresaApi): VacanteResumen {
  return {
    id: v.id,
    puesto: v.puesto,
    area: AREAS[v.area] ?? v.area,
    modalidad: v.modalidad as Modalidad,
    sueldo: v.sueldo,
    fechaCierre: v.fechaCierre,
    descripcion: v.descripcion,
    queHara: v.queHara,
    requisitos: v.requisitos.map(aRequisitoWeb),
    evaluacionActiva: v.evaluacionActiva,
    creada: v.creada,
    estado: v.estado === "CERRADA" ? "CERRADA" : "ACTIVA",
    diasParaCierre: v.diasParaCierre,
    postulantes: v.postulantes,
    evaluados: v.evaluados,
    confirmados: v.confirmados,
  };
}

export function aCrearVacanteApi(v: Omit<Vacante, "id" | "creada">) {
  return {
    puesto: v.puesto,
    area: areaACodigo(v.area),
    modalidad: v.modalidad,
    sueldo: v.sueldo,
    fechaCierre: v.fechaCierre,
    descripcion: v.descripcion,
    queHara: v.queHara,
    requisitos: v.requisitos.map((r) => ({ habilidad: r.habilidad, nivel: r.nivel, tipo: r.tipo, filtro: r.filtro })),
    evaluacionActiva: v.evaluacionActiva,
    preguntasFiltro: v.evaluacionActiva ? (v.preguntasFiltro ?? []) : [],
  };
}

export function aPostulanteWeb(p: PostulanteApi): Postulante {
  return {
    id: p.id,
    vacanteId: p.vacanteId,
    nombre: p.nombre,
    carrera: p.carrera,
    universidad: p.universidad,
    ciclo: p.ciclo,
    fechaPostulacion: p.fecha,
    evaluado: p.evaluado,
    preguntasCorrectas: p.preguntasCorrectas,
    sellos: p.sellos as Record<string, TipoSello>,
    evidencias: [], // Las evidencias las valida el equipo; la empresa ve su efecto en los sellos.
    estado: p.estado as EstadoPostulacion,
    motivoDescarte: p.motivoDescarte,
    cvArchivoId: p.cvArchivoId ?? null,
    preguntasSolicitadas: p.preguntasSolicitadas ?? false,
  };
}

const ESTADOS_VERIFICACION: readonly Empresa["verificacion"][] = ["SIN_VERIFICAR", "EN_REVISION", "VERIFICADA"];

/**
 * Perfil del backend → web. Si falta un campo o llega un estado desconocido, usa un valor seguro
 * en vez de dejar `undefined` (eso rompía la pantalla de perfil).
 */
export function aEmpresaWeb(e: Partial<EmpresaApi>): Empresa {
  const verificacion = ESTADOS_VERIFICACION.find((estado) => estado === e.verificacion) ?? "SIN_VERIFICAR";
  return {
    razonSocial: e.razonSocial ?? "",
    ruc: e.ruc ?? "",
    sector: e.sector || "Otro",
    web: e.web ?? "",
    tamano: e.tamano || "1 a 10 trabajadores",
    contactoNombre: e.contactoNombre ?? "",
    contactoCorreo: e.contactoCorreo ?? "",
    contactoTelefono: e.contactoTelefono ?? "",
    descripcion: e.descripcion ?? "",
    logo: null,
    verificacion,
    logoArchivoId: e.logoArchivoId ?? null,
  };
}

export function aEntrevistaWeb(e: EntrevistaApi): Entrevista {
  return {
    id: e.id,
    postulanteId: e.postulacionId,
    vacanteId: e.vacanteId,
    fecha: e.fecha,
    hora: e.hora,
    duracionMin: e.duracionMin,
    modalidad: e.modalidad as Entrevista["modalidad"],
    mensaje: e.mensaje,
    nombrePostulante: e.nombrePostulante,
    puesto: e.puesto,
  };
}

/** Perfil vacío para una empresa que todavía no completa E01 (la API responde 404). */
const empresaVacia: Empresa = {
  razonSocial: "", ruc: "", sector: "Otro", web: "", tamano: "1 a 10 trabajadores", contactoNombre: "",
  contactoCorreo: "", contactoTelefono: "", descripcion: "", logo: null, verificacion: "SIN_VERIFICAR",
};

// ---------- Implementación ----------

export const apiReal: ApiEmpresa = {
  esReal: true,

  async empresa() {
    try {
      return aEmpresaWeb(await pedir<EmpresaApi>("GET", "empresa/perfil"));
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 404) return empresaVacia;
      throw e;
    }
  },

  async guardarEmpresa(datos) {
    // El logo no viaja aquí: se sube aparte con subirLogo (F7).
    const { logo: _logo, logoArchivoId: _logoId, ...resto } = datos;
    void _logo;
    void _logoId;
    return aEmpresaWeb(await pedir<EmpresaApi>("PUT", "empresa/perfil", resto));
  },

  async subirLogo(archivo) {
    const subido = await subirArchivo("LOGO", archivo);
    return aEmpresaWeb(await pedir<EmpresaApi>("PUT", "empresa/logo", { archivoId: subido.id }));
  },

  async urlArchivo(archivoId) {
    return URL.createObjectURL(await descargarArchivo(archivoId));
  },

  simularVerificacion: () => Promise.reject(new Error("La verificación la hace el equipo de ChambaReady.")),

  saldo: () => pedir<SaldoEvaluaciones>("GET", "empresa/saldo"),

  async configuracion(): Promise<Configuracion> {
    const precios = await pedir<{ planEmpresaCentimos: number }>("GET", "configuracion/precios");
    return { precioPlanCentimos: precios.planEmpresaCentimos };
  },

  async vacantes() {
    return (await pedir<VacanteEmpresaApi[]>("GET", "empresa/vacantes")).map(aVacanteWeb);
  },

  crearVacante: (v) => pedir("POST", "empresa/vacantes", aCrearVacanteApi(v)),

  sugerirPreguntas: (puesto, requisitos) =>
    pedir<PreguntasSugeridas>("POST", "empresa/preguntas-filtro/sugerir", {
      puesto,
      requisitos: requisitos.map((r) => ({ habilidad: r.habilidad, nivel: r.nivel, tipo: r.tipo, filtro: r.filtro })),
    }),

  async postulantes(vacanteId) {
    return (await pedir<PostulanteApi[]>("GET", `empresa/vacantes/${encodeURIComponent(vacanteId)}/postulantes`)).map(aPostulanteWeb);
  },

  async postulante(postulanteId) {
    return aPostulanteWeb(await pedir<PostulanteApi>("GET", `empresa/postulaciones/${encodeURIComponent(postulanteId)}`));
  },

  cambiarEstado: (postulanteId, estado, motivo) =>
    pedir("PATCH", `empresa/postulaciones/${encodeURIComponent(postulanteId)}`, { estado, motivo }),

  enviarPreguntas: (postulanteId) => pedir("POST", `empresa/postulaciones/${encodeURIComponent(postulanteId)}/enviar-preguntas`),

  async entrevistas() {
    return (await pedir<EntrevistaApi[]>("GET", "empresa/entrevistas")).map(aEntrevistaWeb);
  },

  agendarEntrevista: (e) =>
    pedir("POST", "empresa/entrevistas", {
      postulacionId: e.postulanteId, fecha: e.fecha, hora: e.hora, duracionMin: e.duracionMin, modalidad: e.modalidad, mensaje: e.mensaje,
    }),

  actividad: () => pedir<Actividad[]>("GET", "empresa/actividad"),

  estadoPlan: () => pedir<EstadoPlan>("GET", "empresa/plan"),

  pagarPlan: () => pedir<EstadoPlan>("POST", "empresa/plan/pago-simulado"),

  notificaciones: () => pedir<Notificacion[]>("GET", "notificaciones"),

  marcarLeida: (id) => pedir("POST", `notificaciones/${encodeURIComponent(id)}/leida`),

  marcarTodas: () => pedir("POST", "notificaciones/leer-todas"),

  restablecer: () => Promise.reject(new Error("Solo disponible con los datos de prueba.")),
};
