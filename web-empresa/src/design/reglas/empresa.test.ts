import { describe, expect, it } from "vitest";
import { calcularAjuste, formatoSoles, horariosDelDia, precioPorEvaluadoCentimos, validarPreguntasFiltro, validarRuc } from "./empresa";

describe("RUC (E01)", () => {
  it("acepta un RUC real con dígito verificador correcto", () => {
    expect(validarRuc("20131312955")).toBeNull();
    expect(validarRuc(" 20100123453 ")).toBeNull();
  });

  it("exige 11 dígitos", () => {
    expect(validarRuc("2013131295")).toMatch(/11 dígitos/);
    expect(validarRuc("2013131295A")).toMatch(/11 dígitos/);
  });

  it("exige un prefijo válido", () => {
    expect(validarRuc("30131312955")).toMatch(/empezar/);
  });

  it("detecta un dígito verificador equivocado", () => {
    expect(validarRuc("20131312956")).toMatch(/último dígito/);
  });
});

describe("% de ajuste (E04)", () => {
  const requisitos = [
    { id: "r1", tipo: "OBLIGATORIO" as const, filtro: true },
    { id: "r2", tipo: "OBLIGATORIO" as const, filtro: false },
    { id: "r3", tipo: "DESEABLE" as const, filtro: false },
  ];

  it("todo confirmado es 100", () => {
    expect(calcularAjuste(requisitos, { r1: "CONFIRMADO", r2: "CONFIRMADO", r3: "CONFIRMADO" })).toBe(100);
  });

  it("declarado vale la mitad y el filtro pesa más", () => {
    // (3×1 + 2×0,5 + 1×0) / 6 = 67 %
    expect(calcularAjuste(requisitos, { r1: "CONFIRMADO", r2: "DECLARADO", r3: "NO_CUMPLE" })).toBe(67);
  });

  it("sin requisitos es 100", () => {
    expect(calcularAjuste([], {})).toBe(100);
  });
});

describe("precio por evaluado (E06)", () => {
  it("divide el precio fijo entre los postulantes", () => {
    expect(precioPorEvaluadoCentimos(14_900, 100)).toBe(149);
    expect(precioPorEvaluadoCentimos(14_900, 300)).toBe(50);
    expect(precioPorEvaluadoCentimos(14_900, 0)).toBe(14_900);
  });

  it("formatea en soles", () => {
    expect(formatoSoles(14_900)).toBe("S/ 149.00");
    expect(formatoSoles(50)).toBe("S/ 0.50");
  });
});

describe("horarios de entrevista (E07)", () => {
  const antes = new Date(2026, 10, 1, 8, 0); // otro día, para que nada "ya pasó"

  it("de 9:00 a 18:00 sin la hora de almuerzo", () => {
    const horas = horariosDelDia("2026-11-25", 30, [], antes).map((h) => h.hora);
    expect(horas[0]).toBe("09:00");
    expect(horas.at(-1)).toBe("17:30");
    expect(horas).not.toContain("13:00");
    expect(horas).not.toContain("13:30");
  });

  it("marca ocupados los que se cruzan con otra entrevista", () => {
    const h = horariosDelDia("2026-11-25", 60, [{ fecha: "2026-11-25", hora: "10:00", duracionMin: 30 }], antes);
    const ocupado = (hora: string) => h.find((x) => x.hora === hora)?.ocupado;
    expect(ocupado("09:00")).toBe(false); // 9:00–10:00 termina justo cuando empieza la otra
    expect(ocupado("09:30")).toBe(true);
    expect(ocupado("10:00")).toBe(true);
    expect(ocupado("10:30")).toBe(false);
    expect(ocupado("12:30")).toBe(true); // de 60 min pisaría el almuerzo
  });

  it("si es hoy, los horarios pasados están ocupados", () => {
    const ahora = new Date(2026, 10, 25, 11, 10);
    const h = horariosDelDia("2026-11-25", 30, [], ahora);
    expect(h.find((x) => x.hora === "11:00")?.ocupado).toBe(true);
    expect(h.find((x) => x.hora === "11:30")?.ocupado).toBe(false);
  });
});

describe("preguntas filtro (E03, F6)", () => {
  const buena = { requisitoIndice: 0, enunciado: "¿Qué cláusula filtra filas?", opciones: ["WHERE", "HAVING", "ORDER BY", "LIMIT"], indiceCorrecto: 0 };

  it("una pregunta completa es válida", () => {
    expect(validarPreguntasFiltro([buena], 2)).toBeNull();
    expect(validarPreguntasFiltro([], 2)).toBeNull();
  });

  it("se rechazan opciones repetidas, vacías o un requisito que no existe", () => {
    expect(validarPreguntasFiltro([{ ...buena, opciones: ["a", "A", "b", "c"] }], 1)).toMatch(/distintas/);
    expect(validarPreguntasFiltro([{ ...buena, opciones: ["a", "", "b", "c"] }], 1)).toMatch(/4 opciones/);
    expect(validarPreguntasFiltro([{ ...buena, requisitoIndice: 3 }], 2)).toMatch(/requisito/);
    expect(validarPreguntasFiltro([{ ...buena, enunciado: "corta" }], 1)).toMatch(/enunciado/);
  });

  it("máximo dos preguntas", () => {
    expect(validarPreguntasFiltro([buena, buena, buena], 3)).toMatch(/Máximo 2/);
  });
});
