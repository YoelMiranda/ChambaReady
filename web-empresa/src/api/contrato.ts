import type {
  Actividad, Configuracion, Empresa, Entrevista, EstadoPlan, EstadoPostulacion, Notificacion, Postulante, PreguntasSugeridas,
  Requisito, SaldoEvaluaciones, Vacante, VacanteResumen,
} from "./tipos";

/**
 * Lo que las pantallas pueden pedirle a la API. Lo cumplen la API simulada (datos en el navegador)
 * y la API real (backend /v1). Así las pantallas no saben cuál de las dos están usando.
 */
export interface ApiEmpresa {
  /** True si es la API real. Algunas acciones de demostración se ocultan en ese caso. */
  readonly esReal: boolean;
  empresa(): Promise<Empresa>;
  guardarEmpresa(datos: Empresa): Promise<Empresa>;
  simularVerificacion(): Promise<Empresa>;
  /** F7: sube el logo y lo asigna a la empresa. */
  subirLogo(archivo: File): Promise<Empresa>;
  /** F7: URL local (blob:) para mostrar o abrir un archivo protegido (logo, CV). */
  urlArchivo(archivoId: string): Promise<string>;
  saldo(): Promise<SaldoEvaluaciones>;
  configuracion(): Promise<Configuracion>;
  vacantes(): Promise<VacanteResumen[]>;
  crearVacante(vacante: Omit<Vacante, "id" | "creada">): Promise<unknown>;
  /** F6: preguntas filtro sugeridas por la IA para revisarlas antes de publicar. */
  sugerirPreguntas(puesto: string, requisitos: Requisito[]): Promise<PreguntasSugeridas>;
  postulantes(vacanteId: string): Promise<Postulante[]>;
  postulante(postulanteId: string): Promise<Postulante>;
  cambiarEstado(postulanteId: string, estado: EstadoPostulacion, motivo: string | null): Promise<unknown>;
  enviarPreguntas(postulanteId: string): Promise<unknown>;
  entrevistas(): Promise<Entrevista[]>;
  agendarEntrevista(entrevista: Omit<Entrevista, "id" | "nombrePostulante" | "puesto">): Promise<unknown>;
  actividad(): Promise<Actividad[]>;
  /** F8: estado del Plan Empresa y pagos simulados. */
  estadoPlan(): Promise<EstadoPlan>;
  /** F8: pago SIMULADO (no se cobra nada): activa el plan por 30 días. */
  pagarPlan(): Promise<EstadoPlan>;
  /** F9: notificaciones de la campanita. */
  notificaciones(): Promise<Notificacion[]>;
  marcarLeida(notificacionId: string): Promise<unknown>;
  marcarTodas(): Promise<unknown>;
  restablecer(): Promise<boolean>;
}
