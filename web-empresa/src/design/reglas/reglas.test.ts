import { describe, expect, it } from "vitest";
import { aISO, desdeISO, esDiaAgendable, puedeIrAMesAnterior, semanasDelMes } from "./calendario";
import { textoCuentaRegresiva, urgencia } from "./cuentaRegresiva";

describe("calendario de entrevistas (E07)", () => {
  // Miércoles 18 de noviembre de 2026.
  const hoy = new Date(2026, 10, 18, 15, 30);

  it("no permite días pasados", () => {
    expect(esDiaAgendable(new Date(2026, 10, 17), hoy)).toBe(false);
  });

  it("permite hoy aunque ya sea de tarde", () => {
    expect(esDiaAgendable(new Date(2026, 10, 18), hoy)).toBe(true);
  });

  it("no permite domingos", () => {
    expect(esDiaAgendable(new Date(2026, 10, 22), hoy)).toBe(false);
    expect(esDiaAgendable(new Date(2026, 10, 21), hoy)).toBe(true); // sábado
  });

  it("arma la grilla empezando en lunes", () => {
    const semanas = semanasDelMes(2026, 10); // noviembre 2026 empieza en domingo
    expect(semanas[0].slice(0, 6)).toEqual([null, null, null, null, null, null]);
    expect(semanas[0][6]?.getDate()).toBe(1);
    expect(semanas.every((s) => s.length === 7)).toBe(true);
    expect(semanas.flat().filter(Boolean)).toHaveLength(30);
  });

  it("no retrocede antes del mes actual", () => {
    expect(puedeIrAMesAnterior(2026, 10, hoy)).toBe(false);
    expect(puedeIrAMesAnterior(2026, 11, hoy)).toBe(true);
    expect(puedeIrAMesAnterior(2027, 0, hoy)).toBe(true);
  });

  it("convierte fechas ida y vuelta", () => {
    expect(aISO(new Date(2026, 0, 5))).toBe("2026-01-05");
    expect(aISO(desdeISO("2026-12-31"))).toBe("2026-12-31");
  });
});

describe("cuenta regresiva (regla 2)", () => {
  it("colorea según los días", () => {
    expect(urgencia(3)).toBe("roja");
    expect(urgencia(4)).toBe("ambar");
    expect(urgencia(7)).toBe("ambar");
    expect(urgencia(8)).toBe("verde");
    expect(urgencia(-1)).toBe("cerrada");
  });

  it("usa textos claros", () => {
    expect(textoCuentaRegresiva(0)).toBe("Cierra hoy");
    expect(textoCuentaRegresiva(1)).toBe("Cierra mañana");
    expect(textoCuentaRegresiva(5)).toBe("Quedan 5 días");
  });
});
