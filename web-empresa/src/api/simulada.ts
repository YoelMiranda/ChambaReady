/**
 * API simulada de F1–F3. Guarda el estado en localStorage para que los cambios sobrevivan al
 * recargar la página. Expone las mismas operaciones que tendrá la API real (`/v1`): en F4, cada
 * función de este archivo se cambia por un `fetch` y las pantallas no se tocan.
 *
 * Datos ficticios: no uses datos de personas o empresas reales.
 */
import { aISO } from "../design/reglas/calendario";
import type { ApiEmpresa } from "./contrato";
import type {
  Actividad,
  Configuracion,
  Empresa,
  Entrevista,
  EstadoPlan,
  EstadoPostulacion,
  Notificacion,
  Pago,
  Postulante,
  PreguntaFiltroNueva,
  Requisito,
  SaldoEvaluaciones,
  Vacante,
  VacanteResumen,
} from "./tipos";
import { ETIQUETA_ESTADO } from "./tipos";

interface EstadoSimulado {
  empresa: Empresa;
  saldo: SaldoEvaluaciones;
  configuracion: Configuracion;
  vacantes: Vacante[];
  postulantes: Postulante[];
  entrevistas: Entrevista[];
  actividad: Actividad[];
  /** Agregados en F8 y F9 (pueden faltar en datos guardados por versiones anteriores). */
  pagos?: Pago[];
  notificaciones?: Notificacion[];
}

const CLAVE = "chambaready-empresa-demo-v1";
const DIAS_POR_PAGO = 30;

/** Preguntas de ejemplo para el modo demo (sin IA): una por requisito, máximo 2. */
function preguntasDeEjemplo(requisitos: Requisito[]): PreguntaFiltroNueva[] {
  const orden = requisitos
    .map((r, i) => ({ r, i, peso: r.filtro ? 0 : r.tipo === "OBLIGATORIO" ? 1 : 2 }))
    .sort((a, b) => a.peso - b.peso)
    .slice(0, 2);
  return orden.map(({ r, i }) => ({
    requisitoIndice: i,
    enunciado: `¿Cuál es la mejor forma de demostrar tu nivel en ${r.habilidad}?`,
    opciones: ["Con un proyecto o certificado verificable", "Diciendo que lo dominas", "Con años de estudio sin práctica", "No se puede demostrar"],
    indiceCorrecto: 0,
  }));
}

function diasDesdeHoy(dias: number): string {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return aISO(d);
}

function estadoInicial(): EstadoSimulado {
  const ahora = new Date().toISOString();
  return {
    empresa: {
      razonSocial: "Banco Andino S.A.A. (ficticia)",
      ruc: "20131312955",
      sector: "Banca y finanzas",
      web: "https://ejemplo.com",
      tamano: "Más de 500 trabajadores",
      contactoNombre: "Equipo de Talento",
      contactoCorreo: "talento@ejemplo.com",
      contactoTelefono: "999 999 999",
      descripcion: "Banco ficticio para probar ChambaReady.",
      logo: null,
      verificacion: "VERIFICADA",
    },
    saldo: { gratisRestantes: 4, gratisTotales: 20, planActivo: false },
    configuracion: { precioPlanCentimos: 14_900 },
    vacantes: [
      {
        id: "ve1",
        puesto: "Practicante de Analítica de Datos",
        area: "Data y Analytics",
        modalidad: "HIBRIDA",
        sueldo: "S/ 1,300",
        fechaCierre: diasDesdeHoy(2),
        descripcion: "Apoyo al equipo de Inteligencia Comercial.",
        queHara: ["Preparar reportes semanales con SQL y Excel.", "Actualizar tableros de seguimiento."],
        requisitos: [
          { id: "ve1r1", habilidad: "SQL", nivel: "INTERMEDIO", tipo: "OBLIGATORIO", filtro: true },
          { id: "ve1r2", habilidad: "Excel", nivel: "AVANZADO", tipo: "OBLIGATORIO", filtro: false },
          { id: "ve1r3", habilidad: "Power BI", nivel: "BASICO", tipo: "DESEABLE", filtro: false },
        ],
        evaluacionActiva: true,
        creada: diasDesdeHoy(-20),
      },
      {
        id: "ve2",
        puesto: "Practicante de Marketing Digital",
        area: "Marketing digital",
        modalidad: "PRESENCIAL",
        sueldo: "S/ 1,025",
        fechaCierre: diasDesdeHoy(6),
        descripcion: "Campañas digitales de temporada.",
        queHara: ["Programar publicaciones.", "Medir resultados de campañas."],
        requisitos: [
          { id: "ve2r1", habilidad: "Marketing digital", nivel: "INTERMEDIO", tipo: "OBLIGATORIO", filtro: true },
          { id: "ve2r2", habilidad: "Google Ads", nivel: "BASICO", tipo: "DESEABLE", filtro: false },
        ],
        evaluacionActiva: true,
        creada: diasDesdeHoy(-12),
      },
      {
        id: "ve3",
        puesto: "Practicante de Finanzas",
        area: "Finanzas",
        modalidad: "HIBRIDA",
        sueldo: "S/ 1,400",
        fechaCierre: diasDesdeHoy(15),
        descripcion: "Soporte a Planeamiento Financiero.",
        queHara: ["Armar modelos de presupuesto."],
        requisitos: [
          { id: "ve3r1", habilidad: "Excel", nivel: "INTERMEDIO", tipo: "OBLIGATORIO", filtro: false },
          { id: "ve3r2", habilidad: "Finanzas", nivel: "BASICO", tipo: "OBLIGATORIO", filtro: false },
        ],
        evaluacionActiva: false,
        creada: diasDesdeHoy(-3),
      },
      {
        id: "ve4",
        puesto: "Practicante de Operaciones",
        area: "Operaciones y logística",
        modalidad: "PRESENCIAL",
        sueldo: null,
        fechaCierre: diasDesdeHoy(-3),
        descripcion: "Control de indicadores de despacho.",
        queHara: ["Consolidar indicadores diarios."],
        requisitos: [{ id: "ve4r1", habilidad: "Excel", nivel: "INTERMEDIO", tipo: "OBLIGATORIO", filtro: false }],
        evaluacionActiva: true,
        creada: diasDesdeHoy(-40),
      },
    ],
    postulantes: [
      postulante("p1", "ve1", "Postulante A", "Ingeniería de Información", true, 2, { ve1r1: "CONFIRMADO", ve1r2: "CONFIRMADO", ve1r3: "DECLARADO" }, "NUEVA", [{ titulo: "Certificado de Excel avanzado", estado: "VALIDADA" }]),
      postulante("p2", "ve1", "Postulante B", "Economía", true, 1, { ve1r1: "DECLARADO", ve1r2: "CONFIRMADO", ve1r3: "NO_CUMPLE" }, "REVISION", []),
      postulante("p3", "ve1", "Postulante C", "Administración", false, null, { ve1r1: "DECLARADO", ve1r2: "DECLARADO", ve1r3: "DECLARADO" }, "NUEVA", []),
      postulante("p4", "ve1", "Postulante D", "Estadística", false, null, { ve1r1: "DECLARADO", ve1r2: "DECLARADO", ve1r3: "NO_CUMPLE" }, "NUEVA", [{ titulo: "Repositorio de análisis", estado: "EN_REVISION" }]),
      postulante("p5", "ve2", "Postulante E", "Marketing", true, 2, { ve2r1: "CONFIRMADO", ve2r2: "CONFIRMADO" }, "ENTREVISTA", []),
      postulante("p6", "ve2", "Postulante F", "Comunicaciones", true, 0, { ve2r1: "DECLARADO", ve2r2: "DECLARADO" }, "NUEVA", []),
      postulante("p7", "ve3", "Postulante G", "Contabilidad", false, null, { ve3r1: "DECLARADO", ve3r2: "DECLARADO" }, "NUEVA", []),
    ],
    entrevistas: [
      { id: "en1", postulanteId: "p5", vacanteId: "ve2", fecha: diasDesdeHoy(2), hora: "10:00", duracionMin: 45, modalidad: "VIDEOLLAMADA", mensaje: "Te esperamos por videollamada." },
    ],
    actividad: [
      { id: "a1", texto: "Postulante D postuló a Analítica de Datos (sin evaluar)", fecha: ahora },
      { id: "a2", texto: "Postulante A respondió las preguntas filtro (2 de 2)", fecha: ahora },
      { id: "a3", texto: "Agendaste una entrevista con Postulante E", fecha: ahora },
    ],
  };
}

function postulante(
  id: string,
  vacanteId: string,
  nombre: string,
  carrera: string,
  evaluado: boolean,
  preguntasCorrectas: number | null,
  sellos: Postulante["sellos"],
  estado: EstadoPostulacion,
  evidencias: Postulante["evidencias"],
): Postulante {
  return {
    id, vacanteId, nombre, carrera, universidad: "Universidad ficticia", ciclo: 7,
    fechaPostulacion: diasDesdeHoy(-2), evaluado, preguntasCorrectas, sellos, evidencias, estado, motivoDescarte: null,
  };
}

// ---------- Persistencia ----------

let estado: EstadoSimulado = cargar();

function cargar(): EstadoSimulado {
  try {
    const texto = localStorage.getItem(CLAVE);
    if (texto) return JSON.parse(texto) as EstadoSimulado;
  } catch {
    // Sin acceso a localStorage (modo privado) o datos dañados: se empieza de cero.
  }
  return estadoInicial();
}

function guardar() {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(estado));
  } catch {
    // Si no se puede guardar, los cambios viven solo hasta recargar.
  }
}

/** Simula la demora de red para ver los estados de carga. */
function responder<T>(valor: T, ms = 200): Promise<T> {
  return new Promise((resolver) => setTimeout(() => resolver(structuredClone(valor)), ms));
}

function notificar(tipo: string, titulo: string, cuerpo: string) {
  const nueva: Notificacion = { id: `n${Date.now()}`, tipo, titulo, cuerpo, vacanteId: null, leida: false, fecha: new Date().toISOString() };
  estado.notificaciones = [nueva, ...(estado.notificaciones ?? [])].slice(0, 50);
}

function registrarActividad(texto: string) {
  estado.actividad = [{ id: `a${Date.now()}`, texto, fecha: new Date().toISOString() }, ...estado.actividad].slice(0, 20);
}

function diasHasta(fecha: string): number {
  const hoy = new Date(new Date().toDateString());
  const [a, m, d] = fecha.split("-").map(Number);
  return Math.round((new Date(a, m - 1, d).getTime() - hoy.getTime()) / 86_400_000);
}

// ---------- Operaciones (en F4: llamadas a /v1) ----------

export const apiSimulada: ApiEmpresa = {
  esReal: false,

  empresa: () => responder(estado.empresa),

  guardarEmpresa(datos: Empresa) {
    // Cambiar el RUC obliga a verificar de nuevo: sin verificación no se publica.
    const cambioRuc = datos.ruc.trim() !== estado.empresa.ruc;
    estado.empresa = { ...datos, ruc: datos.ruc.trim(), verificacion: cambioRuc ? "EN_REVISION" : estado.empresa.verificacion };
    guardar();
    return responder(estado.empresa);
  },

  /** Solo para la demo: en producción la verificación la hace el equipo de ChambaReady. */
  simularVerificacion() {
    estado.empresa.verificacion = "VERIFICADA";
    guardar();
    return responder(estado.empresa);
  },

  saldo: () => responder(estado.saldo),
  configuracion: () => responder(estado.configuracion),

  vacantes(): Promise<VacanteResumen[]> {
    const lista = estado.vacantes.map((v) => {
      const deVacante = estado.postulantes.filter((p) => p.vacanteId === v.id);
      const dias = diasHasta(v.fechaCierre);
      return {
        ...v,
        estado: dias < 0 ? "CERRADA" : "ACTIVA",
        diasParaCierre: dias,
        postulantes: deVacante.length,
        evaluados: deVacante.filter((p) => p.evaluado).length,
        confirmados: deVacante.filter((p) => Object.values(p.sellos).every((s) => s === "CONFIRMADO")).length,
      } satisfies VacanteResumen;
    });
    return responder(lista.sort((a, b) => b.creada.localeCompare(a.creada)));
  },

  crearVacante(vacante: Omit<Vacante, "id" | "creada">) {
    if (estado.empresa.verificacion !== "VERIFICADA") {
      return Promise.reject(new Error("Tu empresa aún no está verificada: no puedes publicar vacantes."));
    }
    const { preguntasFiltro: _preguntas, ...datos } = vacante;
    void _preguntas; // En el modo demo no se guardan: no hay practicantes que respondan.
    const nueva: Vacante = { ...datos, id: `ve${Date.now()}`, creada: aISO(new Date()) };
    estado.vacantes.push(nueva);
    registrarActividad(`Publicaste «${nueva.puesto}»`);
    guardar();
    return responder(nueva);
  },

  postulantes: (vacanteId: string) => responder(estado.postulantes.filter((p) => p.vacanteId === vacanteId)),

  postulante(postulanteId: string) {
    const p = estado.postulantes.find((x) => x.id === postulanteId);
    return p ? responder(p) : Promise.reject(new Error("Postulante no encontrado"));
  },

  cambiarEstado(postulanteId: string, nuevo: EstadoPostulacion, motivo: string | null = null) {
    const p = estado.postulantes.find((x) => x.id === postulanteId);
    if (!p) return Promise.reject(new Error("Postulante no encontrado"));
    if (nuevo === "DESCARTADA" && !motivo?.trim()) return Promise.reject(new Error("El motivo de descarte es obligatorio."));
    p.estado = nuevo;
    p.motivoDescarte = nuevo === "DESCARTADA" ? motivo!.trim() : null;
    registrarActividad(`${p.nombre} pasó a «${ETIQUETA_ESTADO[nuevo]}»`);
    guardar();
    return responder(p);
  },

  /** Regla 13: con el plan activo se envían las preguntas a quienes entraron "Sin evaluar". */
  enviarPreguntas(postulanteId: string) {
    const p = estado.postulantes.find((x) => x.id === postulanteId);
    if (!p) return Promise.reject(new Error("Postulante no encontrado"));
    if (!estado.saldo.planActivo) return Promise.reject(new Error("Necesitas el Plan Empresa para evaluar a este postulante."));
    registrarActividad(`Enviaste las preguntas filtro a ${p.nombre}`);
    guardar();
    return responder(p);
  },

  entrevistas: () =>
    responder(
      estado.entrevistas.map((e) => ({
        ...e,
        nombrePostulante: estado.postulantes.find((p) => p.id === e.postulanteId)?.nombre,
        puesto: estado.vacantes.find((v) => v.id === e.vacanteId)?.puesto,
      })),
    ),

  agendarEntrevista(entrevista: Omit<Entrevista, "id" | "nombrePostulante" | "puesto">) {
    const ocupado = estado.entrevistas.some((e) => e.fecha === entrevista.fecha && e.hora === entrevista.hora);
    if (ocupado) return Promise.reject(new Error("Ese horario ya está ocupado."));
    const nueva: Entrevista = { ...entrevista, id: `en${Date.now()}` };
    estado.entrevistas.push(nueva);
    const p = estado.postulantes.find((x) => x.id === entrevista.postulanteId);
    if (p) {
      p.estado = "ENTREVISTA";
      registrarActividad(`Agendaste una entrevista con ${p.nombre} el ${entrevista.fecha} a las ${entrevista.hora}`);
    }
    guardar();
    return responder(nueva);
  },

  actividad: () => responder(estado.actividad),

  subirLogo(archivo: File) {
    return new Promise<Empresa>((resolver, rechazar) => {
      const lector = new FileReader();
      lector.onload = () => {
        estado.empresa.logo = String(lector.result);
        guardar();
        resolver(structuredClone(estado.empresa));
      };
      lector.onerror = () => rechazar(new Error("No pudimos leer la imagen."));
      lector.readAsDataURL(archivo);
    });
  },

  urlArchivo: () => Promise.reject(new Error("En el modo demo no hay archivos guardados.")),

  sugerirPreguntas: (_puesto: string, requisitos: Requisito[]) => responder({ preguntas: preguntasDeEjemplo(requisitos), conIa: false }, 600),

  estadoPlan(): Promise<EstadoPlan> {
    const pagos = estado.pagos ?? [];
    const vence = pagos[0]?.venceEn ?? null;
    const activo = estado.saldo.planActivo && (vence === null || diasHasta(vence) >= 0);
    return responder({ activo, venceEn: activo ? vence : null, precioCentimos: estado.configuracion.precioPlanCentimos, pagos });
  },

  /** F8 simulado: no se cobra nada. Suma 30 días al vencimiento (o desde hoy si ya venció). */
  pagarPlan() {
    const pagos = estado.pagos ?? [];
    const venceActual = pagos[0]?.venceEn;
    const desde = venceActual && diasHasta(venceActual) > 0 ? new Date(`${venceActual}T12:00:00`) : new Date();
    desde.setDate(desde.getDate() + DIAS_POR_PAGO);
    const pago: Pago = { id: `pg${Date.now()}`, montoCentimos: estado.configuracion.precioPlanCentimos, fecha: aISO(new Date()), venceEn: aISO(desde) };
    estado.pagos = [pago, ...pagos];
    estado.saldo.planActivo = true;
    estado.saldo.planVenceEn = pago.venceEn;
    registrarActividad("Activaste el Plan Empresa (pago simulado)");
    notificar("PAGO", "Plan Empresa activo", `Pago simulado registrado. Evaluaciones ilimitadas hasta el ${pago.venceEn}.`);
    guardar();
    return apiSimulada.estadoPlan();
  },

  notificaciones: () => responder(estado.notificaciones ?? []),

  marcarLeida(id: string) {
    (estado.notificaciones ?? []).forEach((n) => {
      if (n.id === id) n.leida = true;
    });
    guardar();
    return responder(true);
  },

  marcarTodas() {
    (estado.notificaciones ?? []).forEach((n) => (n.leida = true));
    guardar();
    return responder(true);
  },

  restablecer() {
    estado = estadoInicial();
    guardar();
    return responder(true);
  },
};
