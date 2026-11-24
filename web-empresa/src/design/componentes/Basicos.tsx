import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Link } from "react-router";
import { textoCuentaRegresiva, urgencia } from "../reglas/cuentaRegresiva";

/**
 * Ícono de Material Symbols (outlined). Es decorativo: si un botón solo tiene ícono, ponle
 * `aria-label` al botón.
 */
export function Icon({ nombre, className = "" }: { nombre: string; className?: string }) {
  return (
    <span className={`icono ${className}`} aria-hidden="true">
      {nombre}
    </span>
  );
}

type VarianteBoton = "primario" | "secundario" | "cta";

interface BotonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteBoton;
  icono?: string;
}

/**
 * Botón (46 px de alto). La variante `cta` es el verde de venta y lleva una flecha al final.
 */
export function Button({ variante = "primario", icono, children, className = "", type = "button", ...resto }: BotonProps) {
  return (
    <button type={type} className={`boton boton--${variante} ${className}`} {...resto}>
      {icono && <Icon nombre={icono} />}
      {children}
      {variante === "cta" && <Icon nombre="arrow_forward" />}
    </button>
  );
}

/** Igual que [Button] pero navega con React Router (es un enlace real, funciona con "abrir en otra pestaña"). */
export function ButtonLink({
  to,
  variante = "primario",
  icono,
  children,
}: {
  to: string;
  variante?: VarianteBoton;
  icono?: string;
  children: ReactNode;
}) {
  return (
    <Link to={to} className={`boton boton--${variante}`}>
      {icono && <Icon nombre={icono} />}
      {children}
      {variante === "cta" && <Icon nombre="arrow_forward" />}
    </Link>
  );
}

/** Tarjeta de indicador del panel (E02). */
export function KpiCard({ titulo, valor, detalle, icono }: { titulo: string; valor: string | number; detalle?: string; icono: string }) {
  return (
    <section className="tarjeta" aria-label={titulo}>
      <div className="kpi__titulo">
        <Icon nombre={icono} />
        {titulo}
      </div>
      <div className="kpi__valor">{valor}</div>
      {detalle && <div className="kpi__detalle">{detalle}</div>}
    </section>
  );
}

/** Aviso ámbar o rojo. Solo para avisos, nunca como decoración. */
export function Aviso({ tipo, children }: { tipo: "ambar" | "rojo"; children: ReactNode }) {
  return (
    <div className={`aviso aviso--${tipo}`} role="status">
      <Icon nombre={tipo === "rojo" ? "error" : "warning"} />
      <div>{children}</div>
    </div>
  );
}

/** Cuenta regresiva hasta el cierre de una vacante (regla 2). El texto siempre acompaña al color. */
export function CountdownChip({ diasRestantes }: { diasRestantes: number }) {
  const clases: Record<ReturnType<typeof urgencia>, string> = {
    roja: "chip--rojo",
    ambar: "chip--ambar",
    verde: "chip--verde",
    cerrada: "chip--neutro",
  };
  return (
    <span className={`chip ${clases[urgencia(diasRestantes)]}`}>
      <Icon nombre="schedule" />
      {textoCuentaRegresiva(diasRestantes)}
    </span>
  );
}

export type TipoSello = "CONFIRMADO" | "DECLARADO" | "NO_CUMPLE";

const SELLOS: Record<TipoSello, { etiqueta: string; clase: string; icono: string }> = {
  CONFIRMADO: { etiqueta: "Confirmado", clase: "sello--confirmado", icono: "check_circle" },
  DECLARADO: { etiqueta: "Declarado", clase: "sello--declarado", icono: "info" },
  NO_CUMPLE: { etiqueta: "No cumple", clase: "sello--no-cumple", icono: "remove_circle_outline" },
};

/** Sello de un requisito (E04). Siempre con texto e ícono. */
export function SealBadge({ tipo }: { tipo: TipoSello }) {
  const sello = SELLOS[tipo];
  return (
    <span className={`chip ${sello.clase}`}>
      <Icon nombre={sello.icono} />
      {sello.etiqueta}
    </span>
  );
}

/** Grupo de chips seleccionables (filtros). Usa botones reales con `aria-pressed`. */
export function ChipSelector<T extends string>({
  etiqueta,
  opciones,
  seleccionada,
  onCambio,
}: {
  etiqueta: string;
  opciones: { valor: T; texto: string }[];
  seleccionada: T;
  onCambio: (valor: T) => void;
}) {
  return (
    <div className="selector-chips" role="group" aria-label={etiqueta}>
      {opciones.map((opcion) => (
        <button
          key={opcion.valor}
          type="button"
          aria-pressed={opcion.valor === seleccionada}
          onClick={() => onCambio(opcion.valor)}
        >
          {opcion.texto}
        </button>
      ))}
    </div>
  );
}

/** "Paso 1 de 3" con barra segmentada (E03). */
export function StepProgress({ pasoActual, totalPasos }: { pasoActual: number; totalPasos: number }) {
  return (
    <div>
      <div className="pasos__texto">
        Paso {pasoActual} de {totalPasos}
      </div>
      <div className="pasos__barra" aria-hidden="true">
        {Array.from({ length: totalPasos }, (_, i) => (
          <span key={i} className={i < pasoActual ? "hecho" : ""} />
        ))}
      </div>
    </div>
  );
}
