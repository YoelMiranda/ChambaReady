import { useState } from "react";
import { Link, useParams } from "react-router";
import { api } from "../../api/fuente";
import { useCambiarEstado, useEnviarPreguntas, usePostulantes, useSaldoEvaluaciones, useVacantesEmpresa } from "../../api/consultas";
import type { EstadoPostulacion, Postulante, VacanteResumen } from "../../api/tipos";
import { ETIQUETA_ESTADO, ETIQUETA_NIVEL } from "../../api/tipos";
import { Button, ButtonLink, ChipSelector, Icon, SealBadge } from "../../design/componentes/Basicos";
import { AreaTexto, Dialogo } from "../../design/componentes/Formularios";
import { calcularAjuste, type TipoSello } from "../../design/reglas/empresa";

type Filtro = "TODOS" | "EVALUADOS" | "SIN_EVALUAR";
const ESTADOS_SELECCIONABLES: EstadoPostulacion[] = ["NUEVA", "REVISION", "ENTREVISTA", "ELEGIDA"];
const COLOR_SELLO: Record<TipoSello, string> = {
  CONFIRMADO: "var(--cr-sello-confirmado)",
  DECLARADO: "var(--cr-sello-declarado)",
  NO_CUMPLE: "var(--cr-sello-no-cumple)",
};

/**
 * E04 Candidatos: lista + detalle en la misma página. La vacante y el postulante viven en la URL
 * (/candidatos/:vacanteId/:postulanteId): el botón atrás del navegador y los links compartidos funcionan.
 */
export function CandidatosPage() {
  const { vacanteId, postulanteId } = useParams();
  const { data: vacantes } = useVacantesEmpresa();
  const { data: postulantes, isPending } = usePostulantes(vacanteId);
  const [filtro, setFiltro] = useState<Filtro>("TODOS");
  const vacante = vacantes?.find((v) => v.id === vacanteId);

  if (!vacanteId) {
    return (
      <>
        <div className="encabezado-pagina">
          <div>
            <h1>Candidatos</h1>
            <p>Elige una vacante para ver a sus postulantes.</p>
          </div>
        </div>
        <section className="tarjeta">
          <ul className="lista-simple">
            {vacantes?.map((v) => (
              <li key={v.id}>
                <Link to={`/candidatos/${v.id}`}>
                  {v.puesto}
                  <span className="texto-secundario">
                    {v.postulantes} postulantes · {v.evaluados} evaluados
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </>
    );
  }

  // Orden por % de ajuste (solo vale para esta vacante).
  const conAjuste = (postulantes ?? [])
    .map((p) => ({ p, ajuste: vacante ? calcularAjuste(vacante.requisitos, p.sellos) : 0 }))
    .sort((a, b) => b.ajuste - a.ajuste);
  const visibles = conAjuste.filter(({ p }) => filtro === "TODOS" || (filtro === "EVALUADOS" ? p.evaluado : !p.evaluado));
  const seleccionado = conAjuste.find(({ p }) => p.id === postulanteId);

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Candidatos</h1>
          <p>
            {vacante?.puesto ?? "Vacante"} · El orden vale solo para esta vacante. No existe un ranking global.
          </p>
        </div>
        <ButtonLink to="/candidatos" variante="secundario" icono="arrow_back">
          Otras vacantes
        </ButtonLink>
      </div>

      <div className="dos-columnas">
        <section className="tarjeta pila" aria-label="Lista de postulantes">
          <ChipSelector<Filtro>
            etiqueta="Filtrar postulantes"
            seleccionada={filtro}
            onCambio={setFiltro}
            opciones={[
              { valor: "TODOS", texto: `Todos (${conAjuste.length})` },
              { valor: "EVALUADOS", texto: `Evaluados (${conAjuste.filter(({ p }) => p.evaluado).length})` },
              { valor: "SIN_EVALUAR", texto: `Sin evaluar (${conAjuste.filter(({ p }) => !p.evaluado).length})` },
            ]}
          />
          {isPending && <p className="texto-secundario">Cargando postulantes…</p>}
          {!isPending && visibles.length === 0 && <p className="texto-secundario">No hay postulantes con este filtro.</p>}
          <ul className="lista-candidatos">
            {visibles.map(({ p, ajuste }) => (
              <li key={p.id}>
                <Link to={`/candidatos/${vacanteId}/${p.id}`} aria-current={p.id === postulanteId ? "page" : undefined}>
                  <span style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                    <strong>{p.nombre}</strong>
                    <span>{ajuste} % de ajuste</span>
                  </span>
                  <span className="texto-secundario">{p.carrera} · {ETIQUETA_ESTADO[p.estado]}</span>
                  {vacante && <BarraSellos vacante={vacante} postulante={p} />}
                  {!p.evaluado && (
                    <span className="chip chip--neutro">
                      <Icon nombre="lock" /> Sin evaluar
                    </span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section className="tarjeta pila" aria-label="Detalle del postulante">
          {seleccionado && vacante ? (
            <DetallePostulante postulante={seleccionado.p} ajuste={seleccionado.ajuste} vacante={vacante} />
          ) : (
            <p className="texto-secundario">Elige un postulante para ver sus sellos, sus respuestas y sus evidencias.</p>
          )}
        </section>
      </div>
    </>
  );
}

/** Mini barra con un segmento por requisito; se describe en texto para lectores de pantalla. */
function BarraSellos({ vacante, postulante }: { vacante: VacanteResumen; postulante: Postulante }) {
  const sellos = vacante.requisitos.map((r) => postulante.sellos[r.id] ?? "DECLARADO");
  const resumen = `${sellos.filter((s) => s === "CONFIRMADO").length} confirmados, ${sellos.filter((s) => s === "DECLARADO").length} declarados, ${sellos.filter((s) => s === "NO_CUMPLE").length} no cumple`;
  return (
    <span className="barra-sellos" role="img" aria-label={`Sellos: ${resumen}`}>
      {sellos.map((s, i) => (
        <span key={i} style={{ background: COLOR_SELLO[s] }} />
      ))}
    </span>
  );
}

function DetallePostulante({ postulante: p, ajuste, vacante }: { postulante: Postulante; ajuste: number; vacante: VacanteResumen }) {
  const { data: saldo } = useSaldoEvaluaciones();
  const cambiarEstado = useCambiarEstado();
  const enviarPreguntas = useEnviarPreguntas();
  const [descartando, setDescartando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [errorMotivo, setErrorMotivo] = useState<string | null>(null);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [abriendoCv, setAbriendoCv] = useState(false);

  /** F7: el CV está protegido; se descarga con el token y se abre en otra pestaña. */
  const verCv = async (archivoId: string) => {
    // La pestaña se abre ANTES de esperar la descarga: si no, el navegador la bloquea como popup.
    const pestana = window.open("", "_blank");
    setAbriendoCv(true);
    try {
      const url = await api.urlArchivo(archivoId);
      if (pestana) pestana.location.href = url;
      else window.location.href = url;
    } catch (e) {
      pestana?.close();
      setMensaje(e instanceof Error ? e.message : "No pudimos abrir el CV.");
    } finally {
      setAbriendoCv(false);
    }
  };

  const confirmarDescarte = () => {
    if (motivo.trim().length < 10) {
      setErrorMotivo("Escribe el motivo (10 caracteres como mínimo). El practicante lo verá para poder mejorar.");
      return;
    }
    cambiarEstado.mutate(
      { postulanteId: p.id, estado: "DESCARTADA", motivo },
      { onSuccess: () => { setDescartando(false); setMotivo(""); setMensaje("Postulante descartado. Le mostraremos tu motivo."); } },
    );
  };

  return (
    <>
      <div>
        <h2>{p.nombre}</h2>
        <p className="texto-secundario">
          {p.carrera} · ciclo {p.ciclo} · {p.universidad}
        </p>
        <p>
          <strong>{ajuste} % de ajuste</strong> a esta vacante
        </p>
        {p.cvArchivoId ? (
          <Button variante="secundario" icono="description" onClick={() => void verCv(p.cvArchivoId!)} disabled={abriendoCv}>
            {abriendoCv ? "Abriendo CV…" : "Ver CV"}
          </Button>
        ) : (
          api.esReal && <p className="texto-secundario">No subió su CV.</p>
        )}
      </div>

      <div>
        <h3>Requisitos</h3>
        {vacante.requisitos.map((r) => (
          <div key={r.id} className="fila-requisito">
            <span>
              {r.habilidad} · {ETIQUETA_NIVEL[r.nivel]}
              {r.filtro && <span className="chip chip--rojo" style={{ marginLeft: 8 }}>Filtro</span>}
            </span>
            <SealBadge tipo={p.sellos[r.id] ?? "DECLARADO"} />
          </div>
        ))}
      </div>

      <div>
        <h3>Preguntas filtro</h3>
        {p.evaluado ? (
          <p>
            Respondió <strong>{p.preguntasCorrectas ?? 0} de 2</strong> correctamente.
          </p>
        ) : (
          <div className="bloqueado pila">
            <span>
              <Icon nombre="lock" /> <strong>Sin evaluar.</strong> Entró cuando no te quedaban evaluaciones: sus requisitos están solo declarados y no puedes comprobarlos.
            </span>
            {p.preguntasSolicitadas ? (
              <span className="chip chip--ambar">
                <Icon nombre="hourglass_empty" /> Preguntas enviadas: esperando su respuesta
              </span>
            ) : saldo?.planActivo ? (
              <div>
                <Button
                  variante="secundario"
                  icono="send"
                  disabled={enviarPreguntas.isPending}
                  onClick={() =>
                    enviarPreguntas.mutate(p.id, {
                      onSuccess: () => setMensaje("Le enviamos las preguntas filtro. Le avisamos en la app."),
                      onError: (e) => setMensaje(e.message),
                    })
                  }
                >
                  Enviarle las preguntas filtro
                </Button>
              </div>
            ) : (
              <div>
                <ButtonLink to="/plan" variante="cta">
                  Evalúa a todos tus postulantes
                </ButtonLink>
              </div>
            )}
          </div>
        )}
      </div>

      <div>
        <h3>Evidencias</h3>
        {p.evidencias.length === 0 ? (
          <p className="texto-secundario">No subió evidencias.</p>
        ) : (
          <ul className="lista-viñetas">
            {p.evidencias.map((e) => (
              <li key={e.titulo}>
                {e.titulo} · {e.estado === "VALIDADA" ? "Validada" : "En revisión"}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="pila">
        <h3>Estado</h3>
        {p.estado === "DESCARTADA" ? (
          <p>
            Descartado. Motivo: <em>{p.motivoDescarte}</em>
          </p>
        ) : (
          <ChipSelector<EstadoPostulacion>
            etiqueta="Estado de la postulación"
            seleccionada={p.estado}
            onCambio={(estado) => cambiarEstado.mutate({ postulanteId: p.id, estado })}
            opciones={ESTADOS_SELECCIONABLES.map((e) => ({ valor: e, texto: ETIQUETA_ESTADO[e] }))}
          />
        )}
        <div className="fila-acciones">
          {p.estado !== "DESCARTADA" && (
            <>
              <ButtonLink to={`/entrevistas/agendar/${p.id}`} icono="event">
                Agendar entrevista
              </ButtonLink>
              <Button variante="secundario" icono="block" onClick={() => setDescartando(true)}>
                Descartar
              </Button>
            </>
          )}
          <span role="status" className="estado-guardado">{mensaje}</span>
        </div>
      </div>

      <Dialogo
        abierto={descartando}
        titulo={`Descartar a ${p.nombre}`}
        onCerrar={() => setDescartando(false)}
        acciones={
          <>
            <Button variante="secundario" onClick={() => setDescartando(false)}>
              Cancelar
            </Button>
            <Button onClick={confirmarDescarte} disabled={cambiarEstado.isPending}>
              Descartar
            </Button>
          </>
        }
      >
        <p>El practicante verá este motivo junto con el botón «Mejorar esto». Sé claro y respetuoso.</p>
        <AreaTexto
          etiqueta="Motivo (obligatorio)"
          value={motivo}
          onChange={(e) => { setMotivo(e.target.value); setErrorMotivo(null); }}
          error={errorMotivo}
          placeholder="Ej.: Buscábamos nivel intermedio en SQL."
        />
      </Dialogo>
    </>
  );
}
