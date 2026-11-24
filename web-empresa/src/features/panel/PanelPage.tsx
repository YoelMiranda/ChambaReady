import { Link } from "react-router";
import { useActividad, useSaldoEvaluaciones, useVacantesEmpresa } from "../../api/consultas";
import type { VacanteResumen } from "../../api/tipos";
import { Aviso, ButtonLink, CountdownChip, KpiCard } from "../../design/componentes/Basicos";
import { DataTable } from "../../design/componentes/DataTable";

const UMBRAL_POCAS = 5;
const formatoFechaHora = new Intl.DateTimeFormat("es-PE", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

/** E02 Panel: aviso de evaluaciones, KPIs, tabla de vacantes y actividad reciente. */
export function PanelPage() {
  const { data: saldo } = useSaldoEvaluaciones();
  const { data: vacantes, isPending } = useVacantesEmpresa();
  const { data: actividad } = useActividad();
  const lista = vacantes ?? [];
  const activas = lista.filter((v) => v.estado === "ACTIVA");
  const sumar = (campo: "postulantes" | "evaluados" | "confirmados") => lista.reduce((t, v) => t + v[campo], 0);

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Panel</h1>
          <p>Resumen de tus vacantes y postulantes.</p>
        </div>
        <ButtonLink to="/vacantes/nueva" icono="add">
          Nueva vacante
        </ButtonLink>
      </div>

      {saldo && !saldo.planActivo && saldo.gratisRestantes <= UMBRAL_POCAS && (
        <Aviso tipo="ambar">
          {saldo.gratisRestantes === 0
            ? "Se acabaron tus evaluaciones gratis. Los nuevos postulantes entran «Sin evaluar»: no puedes comprobar sus habilidades."
            : `Te quedan ${saldo.gratisRestantes} evaluaciones gratis. Después, los nuevos postulantes entrarán «Sin evaluar».`}{" "}
          <Link to="/plan">Evalúa a todos tus postulantes</Link>
        </Aviso>
      )}

      <div className="cuadricula-kpi">
        <KpiCard titulo="Vacantes activas" valor={activas.length} icono="work" />
        <KpiCard titulo="Postulantes" valor={vacantes ? sumar("postulantes") : "—"} icono="group" />
        <KpiCard titulo="Evaluados" valor={vacantes ? sumar("evaluados") : "—"} icono="fact_check" detalle="Respondieron las preguntas filtro" />
        <KpiCard titulo="Confirmados" valor={vacantes ? sumar("confirmados") : "—"} icono="verified" detalle="Todos sus requisitos confirmados" />
      </div>

      <h2 style={{ marginBottom: 12 }}>Tus vacantes</h2>
      {isPending ? (
        <p className="texto-secundario">Cargando vacantes…</p>
      ) : (
        <DataTable<VacanteResumen>
          titulo="Tus vacantes"
          filas={lista}
          claveFila={(v) => v.id}
          textoVacio="Aún no publicas vacantes."
          columnas={[
            { clave: "puesto", titulo: "Vacante" },
            { clave: "cierre", titulo: "Cierre", render: (v) => <CountdownChip diasRestantes={v.diasParaCierre} /> },
            { clave: "postulantes", titulo: "Postulantes", alinear: "derecha" },
            { clave: "evaluados", titulo: "Evaluados", alinear: "derecha" },
            { clave: "confirmados", titulo: "Confirmados", alinear: "derecha" },
            { clave: "estado", titulo: "Estado", render: (v) => (v.estado === "ACTIVA" ? "Activa" : "Cerrada") },
            { clave: "accion", titulo: "Acción", render: (v) => <Link to={`/candidatos/${v.id}`}>Ver candidatos</Link> },
          ]}
        />
      )}

      <section className="tarjeta" style={{ marginTop: 24 }}>
        <h2>Actividad reciente</h2>
        <ul className="lista-simple">
          {(actividad ?? []).slice(0, 6).map((a) => (
            <li key={a.id} className="fila-requisito">
              <span>{a.texto}</span>
              <span className="texto-secundario">{formatoFechaHora.format(new Date(a.fecha))}</span>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
