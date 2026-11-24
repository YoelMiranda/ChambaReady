import { describe, expect, it } from "vitest";
import { aCrearVacanteApi, aEmpresaWeb, aEntrevistaWeb, aPostulanteWeb, aRequisitoWeb, areaACodigo, aVacanteWeb } from "./real";

/** Las conversiones entre el backend (/v1) y la web no deben perder ni cambiar datos. */
describe("conversiones de la API real", () => {
  it("un requisito FILTRO se muestra como obligatorio + filtro", () => {
    expect(aRequisitoWeb({ id: "r1", habilidadId: "sql", habilidad: "SQL", nivel: "INTERMEDIO", tipo: "FILTRO" })).toEqual({
      id: "r1", habilidad: "SQL", nivel: "INTERMEDIO", tipo: "OBLIGATORIO", filtro: true,
    });
    expect(aRequisitoWeb({ id: "r2", habilidadId: "bi", habilidad: "Power BI", nivel: "BASICO", tipo: "DESEABLE" }).filtro).toBe(false);
  });

  it("el área viaja como código y se muestra con su nombre", () => {
    expect(areaACodigo("Data y Analytics")).toBe("DATA_ANALYTICS");
    const v = aVacanteWeb({
      id: "v1", puesto: "Analista", area: "FINANZAS", modalidad: "HIBRIDA", sueldo: null, descripcion: "d", queHara: ["a"],
      creada: "2026-11-20T10:00:00Z", fechaCierre: "2026-12-15", diasParaCierre: 21, estado: "ACTIVA", evaluacionActiva: true,
      requisitos: [], postulantes: 3, evaluados: 1, confirmados: 0,
    });
    expect(v.area).toBe("Finanzas");
    expect(v.estado).toBe("ACTIVA");
  });

  it("crear vacante envía el código de área y el filtro aparte", () => {
    const cuerpo = aCrearVacanteApi({
      puesto: "Analista", area: "Marketing digital", modalidad: "REMOTA", sueldo: null, fechaCierre: "2026-12-15",
      descripcion: "Descripción de prueba suficiente.", queHara: ["Tarea"], evaluacionActiva: true,
      requisitos: [{ id: "x", habilidad: "SQL", nivel: "BASICO", tipo: "OBLIGATORIO", filtro: true }],
    });
    expect(cuerpo.area).toBe("MARKETING_DIGITAL");
    expect(cuerpo.requisitos).toEqual([{ habilidad: "SQL", nivel: "BASICO", tipo: "OBLIGATORIO", filtro: true }]);
    expect(cuerpo).not.toHaveProperty("id");
  });

  it("un perfil incompleto o con un estado raro no deja campos undefined", () => {
    const e = aEmpresaWeb({ razonSocial: "Andes SAC", ruc: "20131312955" });
    expect(e.verificacion).toBe("SIN_VERIFICAR");
    expect(e.sector).toBe("Otro");
    expect(e.descripcion).toBe("");
    expect(aEmpresaWeb({ verificacion: "PENDIENTE" }).verificacion).toBe("SIN_VERIFICAR");
    expect(aEmpresaWeb({ verificacion: "EN_REVISION" }).verificacion).toBe("EN_REVISION");
  });

  it("postulante y entrevista se adaptan a los tipos de la web", () => {
    const p = aPostulanteWeb({
      id: "p1", vacanteId: "v1", nombre: "Ana", carrera: "Economía", universidad: "U", ciclo: null, fecha: "2026-11-24",
      evaluado: false, preguntasCorrectas: null, sellos: { r1: "DECLARADO" }, estado: "NUEVA", motivoDescarte: null, ajuste: 50,
    });
    expect(p.fechaPostulacion).toBe("2026-11-24");
    expect(p.evidencias).toEqual([]);
    const e = aEntrevistaWeb({
      id: "e1", postulacionId: "p1", vacanteId: "v1", nombrePostulante: "Ana", puesto: "Analista",
      fecha: "2026-11-25", hora: "10:00", duracionMin: 45, modalidad: "VIDEOLLAMADA", mensaje: "",
    });
    expect(e.postulanteId).toBe("p1");
    expect(e.nombrePostulante).toBe("Ana");
  });
});
