import { useState } from "react";
import { Icon } from "./Basicos";
import { aISO, esDiaAgendable, puedeIrAMesAnterior, semanasDelMes, type FechaISO } from "../reglas/calendario";

const DIAS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];
const formatoMes = new Intl.DateTimeFormat("es-PE", { month: "long", year: "numeric" });
const formatoDia = new Intl.DateTimeFormat("es-PE", { weekday: "long", day: "numeric", month: "long", year: "numeric" });

/**
 * Calendario mensual de E07. Los días pasados y los domingos aparecen deshabilitados.
 * Cada día es un `<button>` con su fecha completa como nombre accesible.
 */
export function Calendar({
  seleccion,
  onSeleccion,
  hoy = new Date(),
}: {
  seleccion: FechaISO | null;
  onSeleccion: (fecha: FechaISO) => void;
  hoy?: Date;
}) {
  const [mesVisible, setMesVisible] = useState({ anio: hoy.getFullYear(), mes0: hoy.getMonth() });
  const { anio, mes0 } = mesVisible;
  const hoyISO = aISO(hoy);

  const moverMes = (delta: number) => {
    const fecha = new Date(anio, mes0 + delta, 1);
    setMesVisible({ anio: fecha.getFullYear(), mes0: fecha.getMonth() });
  };

  return (
    <div className="calendario tarjeta">
      <div className="calendario__cabecera">
        <button
          type="button"
          className="boton-icono"
          aria-label="Mes anterior"
          disabled={!puedeIrAMesAnterior(anio, mes0, hoy)}
          onClick={() => moverMes(-1)}
        >
          <Icon nombre="chevron_left" />
        </button>
        <span className="calendario__mes" aria-live="polite">
          {formatoMes.format(new Date(anio, mes0, 1))}
        </span>
        <button type="button" className="boton-icono" aria-label="Mes siguiente" onClick={() => moverMes(1)}>
          <Icon nombre="chevron_right" />
        </button>
      </div>
      <table>
        <thead>
          <tr>
            {DIAS.map((dia) => (
              <th key={dia} scope="col">
                {dia}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {semanasDelMes(anio, mes0).map((semana, i) => (
            <tr key={i}>
              {semana.map((fecha, j) => {
                if (!fecha) return <td key={j} />;
                const iso = aISO(fecha);
                return (
                  <td key={j}>
                    <button
                      type="button"
                      className={iso === hoyISO ? "hoy" : undefined}
                      disabled={!esDiaAgendable(fecha, hoy)}
                      aria-pressed={iso === seleccion}
                      aria-label={formatoDia.format(fecha)}
                      onClick={() => onSeleccion(iso)}
                    >
                      {fecha.getDate()}
                    </button>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
