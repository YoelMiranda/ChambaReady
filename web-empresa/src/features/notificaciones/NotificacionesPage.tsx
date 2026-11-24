import { Link } from "react-router";
import { useMarcarLeida, useMarcarTodas, useNotificaciones } from "../../api/consultas";
import type { Notificacion } from "../../api/tipos";
import { Button, Icon } from "../../design/componentes/Basicos";

const ICONO: Record<string, string> = {
  NUEVO_POSTULANTE: "person_add",
  PREGUNTAS_FILTRO: "quiz",
  PAGO: "credit_card",
  ENTREVISTA: "event",
};

/** Adónde lleva cada aviso al tocarlo. */
function destino(n: Notificacion): string | null {
  if (n.tipo === "PAGO") return "/plan";
  if (n.vacanteId && (n.tipo === "NUEVO_POSTULANTE" || n.tipo === "PREGUNTAS_FILTRO")) return `/candidatos/${n.vacanteId}`;
  return null;
}

function hace(fechaIso: string): string {
  const minutos = Math.round((Date.now() - new Date(fechaIso).getTime()) / 60_000);
  if (minutos < 1) return "Ahora";
  if (minutos < 60) return `Hace ${minutos} min`;
  const horas = Math.round(minutos / 60);
  if (horas < 24) return `Hace ${horas} h`;
  return new Date(fechaIso).toLocaleDateString("es-PE", { day: "numeric", month: "short" });
}

/**
 * F9 (reducido, sin push): avisos de la empresa dentro de la web. Se actualizan solos cada minuto
 * mientras la página está abierta.
 */
export function NotificacionesPage() {
  const { data: notificaciones, isLoading } = useNotificaciones();
  const marcarLeida = useMarcarLeida();
  const marcarTodas = useMarcarTodas();
  const noLeidas = notificaciones?.filter((n) => !n.leida).length ?? 0;

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Notificaciones</h1>
          <p>Nuevos postulantes, respuestas a las preguntas filtro y pagos.</p>
        </div>
        {noLeidas > 0 && (
          <Button variante="secundario" icono="done_all" onClick={() => marcarTodas.mutate()} disabled={marcarTodas.isPending}>
            Marcar todas como leídas
          </Button>
        )}
      </div>

      <section className="tarjeta">
        {isLoading && <p className="texto-secundario">Cargando…</p>}
        {notificaciones?.length === 0 && <p className="texto-secundario">Todavía no tienes notificaciones.</p>}
        <ul className="lista-notificaciones">
          {notificaciones?.map((n) => {
            const ruta = destino(n);
            const contenido = (
              <>
                <Icon nombre={ICONO[n.tipo] ?? "notifications"} />
                <div>
                  <strong>{n.titulo}</strong>
                  <div>{n.cuerpo}</div>
                  <small className="texto-secundario">{hace(n.fecha)}</small>
                </div>
                {!n.leida && <span className="punto-no-leida" aria-label="No leída" />}
              </>
            );
            const alAbrir = () => {
              if (!n.leida) marcarLeida.mutate(n.id);
            };
            return (
              <li key={n.id} className={n.leida ? "" : "no-leida"}>
                {ruta ? (
                  <Link to={ruta} onClick={alAbrir}>
                    {contenido}
                  </Link>
                ) : (
                  <button type="button" onClick={alAbrir}>
                    {contenido}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      </section>
    </>
  );
}
