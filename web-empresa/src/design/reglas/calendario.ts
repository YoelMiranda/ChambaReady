/**
 * Reglas del calendario de E07 (Agendar entrevista): no se pueden elegir días pasados ni domingos.
 * Funciones puras (sin React) para poder probarlas con Vitest. Las fechas se manejan en hora local.
 */

/** Fecha sin hora en formato "AAAA-MM-DD". */
export type FechaISO = string;

/** Medianoche local del mismo día (quita horas, minutos y segundos). */
export function soloFecha(fecha: Date): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
}

export function aISO(fecha: Date): FechaISO {
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

export function desdeISO(iso: FechaISO): Date {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return new Date(anio, mes - 1, dia);
}

/** True si la fecha es hoy o posterior y no cae domingo. */
export function esDiaAgendable(fecha: Date, hoy: Date): boolean {
  const esDomingo = fecha.getDay() === 0;
  return !esDomingo && soloFecha(fecha).getTime() >= soloFecha(hoy).getTime();
}

/**
 * Semanas del mes para dibujar la grilla, empezando en lunes. Las casillas que no pertenecen al
 * mes valen null. [mes0] va de 0 (enero) a 11 (diciembre).
 */
export function semanasDelMes(anio: number, mes0: number): (Date | null)[][] {
  const diasEnMes = new Date(anio, mes0 + 1, 0).getDate();
  // getDay(): 0 = domingo … 6 = sábado. Lo convertimos a 0 = lunes … 6 = domingo.
  const desfase = (new Date(anio, mes0, 1).getDay() + 6) % 7;
  const casillas: (Date | null)[] = [
    ...Array.from({ length: desfase }, () => null),
    ...Array.from({ length: diasEnMes }, (_, i) => new Date(anio, mes0, i + 1)),
  ];
  while (casillas.length % 7 !== 0) casillas.push(null);
  const semanas: (Date | null)[][] = [];
  for (let i = 0; i < casillas.length; i += 7) semanas.push(casillas.slice(i, i + 7));
  return semanas;
}

/** True si se puede retroceder al mes anterior (nunca antes del mes actual). */
export function puedeIrAMesAnterior(anio: number, mes0: number, hoy: Date): boolean {
  return anio > hoy.getFullYear() || (anio === hoy.getFullYear() && mes0 > hoy.getMonth());
}
