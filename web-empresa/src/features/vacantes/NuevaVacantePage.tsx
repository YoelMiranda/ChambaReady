import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useCrearVacante, useEmpresa, useSaldoEvaluaciones } from "../../api/consultas";
import type { Modalidad, NivelExigido, PreguntaFiltroNueva, Requisito, TipoRequisito } from "../../api/tipos";
import { ETIQUETA_MODALIDAD, ETIQUETA_NIVEL } from "../../api/tipos";
import { Aviso, Button, StepProgress } from "../../design/componentes/Basicos";
import { AreaTexto, Campo, Interruptor, Selector } from "../../design/componentes/Formularios";
import { aISO } from "../../design/reglas/calendario";
import { validarPreguntasFiltro } from "../../design/reglas/empresa";
import { EditorPreguntasFiltro } from "./EditorPreguntasFiltro";

const AREAS = ["Data y Analytics", "Desarrollo de software", "Marketing digital", "Finanzas", "Operaciones y logística", "Diseño UX/UI", "Recursos humanos", "Ventas y comercial"];

interface Borrador {
  puesto: string;
  area: string;
  modalidad: Modalidad;
  sueldo: string;
  fechaCierre: string;
  descripcion: string;
  queHara: string[];
  requisitos: Requisito[];
  evaluacionActiva: boolean;
  preguntasFiltro: PreguntaFiltroNueva[];
}

const nuevoRequisito = (): Requisito => ({ id: `r${Date.now()}${Math.random().toString(36).slice(2, 6)}`, habilidad: "", nivel: "BASICO", tipo: "OBLIGATORIO", filtro: false });

/** Errores por paso (1: datos, 2: qué hará, 3: requisitos). */
function validarPaso(paso: number, b: Borrador, hoy: string): Record<string, string> {
  const e: Record<string, string> = {};
  if (paso === 1) {
    if (b.puesto.trim().length < 5) e.puesto = "Escribe el nombre del puesto.";
    if (!b.fechaCierre) e.fechaCierre = "Elige la fecha de cierre.";
    else if (b.fechaCierre <= hoy) e.fechaCierre = "La fecha de cierre debe ser después de hoy.";
    if (b.descripcion.trim().length < 20) e.descripcion = "Describe la práctica (20 caracteres como mínimo).";
  }
  if (paso === 2 && b.queHara.filter((t) => t.trim()).length === 0) e.queHara = "Agrega al menos una tarea.";
  if (paso === 3) {
    if (b.requisitos.length === 0) e.requisitos = "Agrega al menos un requisito.";
    else if (b.requisitos.some((r) => !r.habilidad.trim())) e.requisitos = "Escribe la habilidad de cada requisito.";
    const errorPreguntas = b.evaluacionActiva ? validarPreguntasFiltro(b.preguntasFiltro, b.requisitos.length) : null;
    if (errorPreguntas) e.preguntas = errorPreguntas;
  }
  return e;
}

/** E03 Nueva vacante en 3 pasos. Publicar es gratis; requiere la empresa verificada. */
export function NuevaVacantePage() {
  const navigate = useNavigate();
  const { data: empresa } = useEmpresa();
  const { data: saldo } = useSaldoEvaluaciones();
  const crear = useCrearVacante();
  const hoy = aISO(new Date());
  const [paso, setPaso] = useState(1);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [b, setB] = useState<Borrador>({
    puesto: "", area: AREAS[0], modalidad: "HIBRIDA", sueldo: "", fechaCierre: "", descripcion: "",
    queHara: [""], requisitos: [nuevoRequisito()], evaluacionActiva: true, preguntasFiltro: [],
  });

  const cambiar = <K extends keyof Borrador>(campo: K, valor: Borrador[K]) => setB({ ...b, [campo]: valor });
  const cambiarRequisito = (id: string, cambios: Partial<Requisito>) =>
    cambiar("requisitos", b.requisitos.map((r) => (r.id === id ? { ...r, ...cambios } : r)));

  const quitarRequisito = (indice: number) =>
    setB({
      ...b,
      requisitos: b.requisitos.filter((_, j) => j !== indice),
      preguntasFiltro: b.preguntasFiltro
        .filter((p) => p.requisitoIndice !== indice)
        .map((p) => (p.requisitoIndice > indice ? { ...p, requisitoIndice: p.requisitoIndice - 1 } : p)),
    });

  const avanzar = () => {
    const e = validarPaso(paso, b, hoy);
    setErrores(e);
    if (Object.keys(e).length === 0) setPaso(paso + 1);
  };

  const publicar = () => {
    const e = validarPaso(3, b, hoy);
    setErrores(e);
    if (Object.keys(e).length > 0) return;
    crear.mutate(
      {
        puesto: b.puesto.trim(), area: b.area, modalidad: b.modalidad, sueldo: b.sueldo.trim() || null, fechaCierre: b.fechaCierre,
        descripcion: b.descripcion.trim(), queHara: b.queHara.map((t) => t.trim()).filter(Boolean),
        requisitos: b.requisitos.map((r) => ({ ...r, habilidad: r.habilidad.trim() })), evaluacionActiva: b.evaluacionActiva,
        preguntasFiltro: b.evaluacionActiva
          ? b.preguntasFiltro.map((p) => ({ ...p, enunciado: p.enunciado.trim(), opciones: p.opciones.map((o) => o.trim()) }))
          : [],
      },
      { onSuccess: () => navigate("/panel") },
    );
  };

  const verificada = empresa?.verificacion === "VERIFICADA";
  const textoSaldo = !saldo ? "" : saldo.planActivo
    ? "Plan Empresa activo: evaluaciones ilimitadas."
    : saldo.gratisRestantes > 0
      ? `Te quedan ${saldo.gratisRestantes} de ${saldo.gratisTotales} evaluaciones gratis. Después, los postulantes entran «Sin evaluar».`
      : "No te quedan evaluaciones gratis: los postulantes entrarán «Sin evaluar» hasta que actives el Plan Empresa.";

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Nueva vacante</h1>
          <p>Publicar es gratis.</p>
        </div>
      </div>

      {empresa && !verificada && (
        <Aviso tipo="rojo">
          Tu empresa aún no está verificada, así que no puedes publicar. <Link to="/perfil">Revisa tu perfil</Link>
        </Aviso>
      )}

      <section className="tarjeta pila">
        <StepProgress pasoActual={paso} totalPasos={3} />

        {paso === 1 && (
          <>
            <h2>Datos de la vacante</h2>
            <div className="cuadricula-form">
              <Campo etiqueta="Puesto" value={b.puesto} onChange={(e) => cambiar("puesto", e.target.value)} error={errores.puesto} placeholder="Practicante de…" />
              <Selector etiqueta="Área" value={b.area} onChange={(e) => cambiar("area", e.target.value)} opciones={AREAS.map((a) => ({ valor: a, texto: a }))} />
              <Selector
                etiqueta="Modalidad"
                value={b.modalidad}
                onChange={(e) => cambiar("modalidad", e.target.value as Modalidad)}
                opciones={(Object.keys(ETIQUETA_MODALIDAD) as Modalidad[]).map((m) => ({ valor: m, texto: ETIQUETA_MODALIDAD[m] }))}
              />
              <Campo etiqueta="Sueldo mensual (opcional)" value={b.sueldo} onChange={(e) => cambiar("sueldo", e.target.value)} placeholder="S/ 1,200" />
              <Campo etiqueta="Fecha de cierre" type="date" min={hoy} value={b.fechaCierre} onChange={(e) => cambiar("fechaCierre", e.target.value)} error={errores.fechaCierre} />
            </div>
            <AreaTexto etiqueta="Descripción" value={b.descripcion} onChange={(e) => cambiar("descripcion", e.target.value)} error={errores.descripcion} />
          </>
        )}

        {paso === 2 && (
          <>
            <h2>Qué hará el practicante</h2>
            {b.queHara.map((tarea, i) => (
              <div key={i} className="fila-acciones">
                <div style={{ flex: 1 }}>
                  <Campo
                    etiqueta={`Tarea ${i + 1}`}
                    value={tarea}
                    onChange={(e) => cambiar("queHara", b.queHara.map((t, j) => (j === i ? e.target.value : t)))}
                  />
                </div>
                {b.queHara.length > 1 && (
                  <Button variante="secundario" icono="delete" aria-label={`Quitar tarea ${i + 1}`} onClick={() => cambiar("queHara", b.queHara.filter((_, j) => j !== i))}>
                    Quitar
                  </Button>
                )}
              </div>
            ))}
            {errores.queHara && <span className="campo__error" role="alert">{errores.queHara}</span>}
            <div>
              <Button variante="secundario" icono="add" onClick={() => cambiar("queHara", [...b.queHara, ""])}>
                Agregar tarea
              </Button>
            </div>
          </>
        )}

        {paso === 3 && (
          <>
            <h2>Requisitos</h2>
            <p className="texto-secundario">
              Un requisito <strong>filtro</strong> es indispensable: quien declare no cumplirlo no puede postular. Las preguntas filtro confirman los más importantes.
            </p>
            <div className="tabla-contenedor">
              <table className="tabla tabla-requisitos">
                <caption className="visualmente-oculto">Requisitos de la vacante</caption>
                <thead>
                  <tr>
                    <th scope="col">Habilidad</th>
                    <th scope="col">Nivel</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Filtro</th>
                    <th scope="col"><span className="visualmente-oculto">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {b.requisitos.map((r, i) => (
                    <tr key={r.id}>
                      <td>
                        <input aria-label={`Habilidad del requisito ${i + 1}`} value={r.habilidad} onChange={(e) => cambiarRequisito(r.id, { habilidad: e.target.value })} placeholder="Ej.: SQL" />
                      </td>
                      <td>
                        <select aria-label={`Nivel del requisito ${i + 1}`} value={r.nivel} onChange={(e) => cambiarRequisito(r.id, { nivel: e.target.value as NivelExigido })}>
                          {(Object.keys(ETIQUETA_NIVEL) as NivelExigido[]).map((n) => <option key={n} value={n}>{ETIQUETA_NIVEL[n]}</option>)}
                        </select>
                      </td>
                      <td>
                        <select
                          aria-label={`Tipo del requisito ${i + 1}`}
                          value={r.tipo}
                          disabled={r.filtro}
                          onChange={(e) => cambiarRequisito(r.id, { tipo: e.target.value as TipoRequisito })}
                        >
                          <option value="OBLIGATORIO">Obligatorio</option>
                          <option value="DESEABLE">Deseable</option>
                        </select>
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          aria-label={`Requisito ${i + 1} es filtro`}
                          checked={r.filtro}
                          onChange={(e) => cambiarRequisito(r.id, { filtro: e.target.checked, tipo: e.target.checked ? "OBLIGATORIO" : r.tipo })}
                          style={{ width: 22, height: 22, minWidth: 0 }}
                        />
                      </td>
                      <td>
                        <button type="button" className="boton-icono" aria-label={`Quitar requisito ${i + 1}`} onClick={() => quitarRequisito(i)}>
                          <span className="icono" aria-hidden="true">delete</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {errores.requisitos && <span className="campo__error" role="alert">{errores.requisitos}</span>}
            <div>
              <Button variante="secundario" icono="add" onClick={() => cambiar("requisitos", [...b.requisitos, nuevoRequisito()])}>
                Agregar requisito
              </Button>
            </div>
            <Interruptor
              etiqueta="Evaluación automática (2 preguntas filtro de 45 s)"
              activo={b.evaluacionActiva}
              onCambio={(v) => cambiar("evaluacionActiva", v)}
              detalle={textoSaldo}
            />
            {b.evaluacionActiva && (
              <EditorPreguntasFiltro
                puesto={b.puesto}
                requisitos={b.requisitos}
                preguntas={b.preguntasFiltro}
                onCambio={(preguntas) => cambiar("preguntasFiltro", preguntas)}
                error={errores.preguntas ?? null}
              />
            )}
          </>
        )}

        {crear.error && <span className="campo__error" role="alert">{crear.error.message}</span>}

        <div className="fila-acciones">
          {paso > 1 && (
            <Button variante="secundario" icono="arrow_back" onClick={() => setPaso(paso - 1)}>
              Anterior
            </Button>
          )}
          {paso < 3 ? (
            <Button onClick={avanzar}>Siguiente</Button>
          ) : (
            <Button onClick={publicar} disabled={!verificada || crear.isPending} icono="publish">
              {crear.isPending ? "Publicando…" : "Publicar vacante"}
            </Button>
          )}
        </div>
      </section>
    </>
  );
}
