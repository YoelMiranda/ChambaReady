import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useAgendarEntrevista, useEntrevistas, usePostulante } from "../../api/consultas";
import type { ModalidadEntrevista } from "../../api/tipos";
import { Button, ButtonLink, ChipSelector, Icon } from "../../design/componentes/Basicos";
import { Calendar } from "../../design/componentes/Calendar";
import { AreaTexto, Selector } from "../../design/componentes/Formularios";
import { desdeISO, type FechaISO } from "../../design/reglas/calendario";
import { horariosDelDia } from "../../design/reglas/empresa";

const formatoFecha = new Intl.DateTimeFormat("es-PE", { weekday: "long", day: "numeric", month: "long" });

/** Lista de entrevistas agendadas (sección "Entrevistas" del menú). */
export function EntrevistasPage() {
  const { data: entrevistas } = useEntrevistas();
  const proximas = [...(entrevistas ?? [])].sort((a, b) => `${a.fecha}${a.hora}`.localeCompare(`${b.fecha}${b.hora}`));

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Entrevistas</h1>
          <p>Para agendar una, abre un candidato y toca «Agendar entrevista».</p>
        </div>
        <ButtonLink to="/candidatos" variante="secundario" icono="groups">
          Ir a candidatos
        </ButtonLink>
      </div>
      <section className="tarjeta">
        {proximas.length === 0 ? (
          <p className="texto-secundario">Aún no tienes entrevistas agendadas.</p>
        ) : (
          <ul className="lista-simple">
            {proximas.map((e) => {
              return (
                <li key={e.id}>
                  <Link to={`/candidatos/${e.vacanteId}/${e.postulanteId}`}>
                    <span>
                      <strong>{e.nombrePostulante ?? "Postulante"}</strong> · {e.puesto ?? ""}
                      <br />
                      <span className="texto-secundario">
                        {formatoFecha.format(desdeISO(e.fecha))} · {e.hora} · {e.duracionMin} min · {e.modalidad === "VIDEOLLAMADA" ? "Videollamada" : "Presencial"}
                      </span>
                    </span>
                    <Icon nombre="chevron_right" />
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </>
  );
}

/**
 * E07 Agendar entrevista: calendario sin días pasados ni domingos, horarios con los ocupados,
 * modalidad, duración y mensaje. Al enviar, el estado pasa a Entrevista y el practicante recibe
 * la notificación con la fecha (FCM en F9).
 */
export function AgendarEntrevistaPage() {
  const { postulanteId } = useParams();
  const navigate = useNavigate();
  const { data: postulante, isError: noEncontrado } = usePostulante(postulanteId);
  const { data: entrevistas } = useEntrevistas();
  const agendar = useAgendarEntrevista();
  const [fecha, setFecha] = useState<FechaISO | null>(null);
  const [hora, setHora] = useState<string | null>(null);
  const [duracion, setDuracion] = useState(30);
  const [modalidad, setModalidad] = useState<ModalidadEntrevista>("VIDEOLLAMADA");
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState<string | null>(null);

  const horarios = fecha ? horariosDelDia(fecha, duracion, entrevistas ?? []) : [];

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!postulante) return;
    if (!fecha || !hora) {
      setError("Elige un día y un horario.");
      return;
    }
    agendar.mutate(
      { postulanteId: postulante.id, vacanteId: postulante.vacanteId, fecha, hora, duracionMin: duracion, modalidad, mensaje: mensaje.trim() },
      {
        onSuccess: () => navigate(`/candidatos/${postulante.vacanteId}/${postulante.id}`),
        onError: (e) => setError(e.message),
      },
    );
  };

  if (noEncontrado) {
    return (
      <section className="tarjeta pila">
        <h1>No encontramos a este postulante</h1>
        <ButtonLink to="/candidatos" variante="secundario">Volver a candidatos</ButtonLink>
      </section>
    );
  }

  return (
    <form onSubmit={enviar} noValidate>
      <div className="encabezado-pagina">
        <div>
          <h1>Agendar entrevista</h1>
          <p>{postulante ? `Con ${postulante.nombre}` : "Cargando…"}</p>
        </div>
        <Button variante="secundario" icono="arrow_back" onClick={() => navigate(-1)}>
          Volver
        </Button>
      </div>

      <div className="dos-columnas">
        <div className="pila">
          <Calendar seleccion={fecha} onSeleccion={(f) => { setFecha(f); setHora(null); setError(null); }} />
          <Selector
            etiqueta="Duración"
            value={String(duracion)}
            onChange={(e) => { setDuracion(Number(e.target.value)); setHora(null); }}
            opciones={[30, 45, 60].map((m) => ({ valor: String(m), texto: `${m} minutos` }))}
          />
        </div>

        <section className="tarjeta pila">
          <h2>{fecha ? formatoFecha.format(desdeISO(fecha)) : "Elige un día"}</h2>
          {fecha && (
            <div className="horarios" role="group" aria-label="Horarios disponibles">
              {horarios.map((h) => (
                <button
                  key={h.hora}
                  type="button"
                  disabled={h.ocupado}
                  aria-pressed={h.hora === hora}
                  aria-label={h.ocupado ? `${h.hora}, ocupado` : h.hora}
                  onClick={() => { setHora(h.hora); setError(null); }}
                >
                  {h.hora}
                </button>
              ))}
            </div>
          )}
          <div className="campo">
            <span className="campo__titulo" id="titulo-modalidad">Modalidad</span>
            <ChipSelector<ModalidadEntrevista>
              etiqueta="Modalidad"
              seleccionada={modalidad}
              onCambio={setModalidad}
              opciones={[{ valor: "VIDEOLLAMADA", texto: "Videollamada" }, { valor: "PRESENCIAL", texto: "Presencial" }]}
            />
          </div>
          <AreaTexto
            etiqueta="Mensaje para el practicante"
            value={mensaje}
            onChange={(e) => setMensaje(e.target.value)}
            placeholder={modalidad === "VIDEOLLAMADA" ? "Te enviaremos el enlace de la videollamada." : "Te esperamos en nuestra oficina de…"}
          />
          {error && <span className="campo__error" role="alert">{error}</span>}
          <div>
            <Button type="submit" icono="send" disabled={agendar.isPending || !postulante}>
              {agendar.isPending ? "Enviando…" : "Enviar invitación"}
            </Button>
          </div>
          <p className="campo__ayuda">El practicante recibirá una notificación con la fecha y su postulación pasará a «Entrevista».</p>
        </section>
      </div>
    </form>
  );
}
