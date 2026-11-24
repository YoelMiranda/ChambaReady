import { useEffect, useId, useRef, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";

/**
 * Controles de formulario accesibles: cada uno tiene su `<label>` real y su error queda conectado
 * con `aria-describedby`, para que el lector de pantalla lo lea al enfocar el campo.
 */

interface BaseCampo {
  etiqueta: string;
  error?: string | null;
  ayuda?: string;
}

function Mensajes({ idAyuda, idError, ayuda, error }: { idAyuda: string; idError: string; ayuda?: string; error?: string | null }) {
  return (
    <>
      {ayuda && (
        <span id={idAyuda} className="campo__ayuda">
          {ayuda}
        </span>
      )}
      {error && (
        <span id={idError} className="campo__error" role="alert">
          {error}
        </span>
      )}
    </>
  );
}

function describedBy(id: string, ayuda?: string, error?: string | null): string | undefined {
  const ids = [ayuda ? `${id}-ayuda` : null, error ? `${id}-error` : null].filter(Boolean);
  return ids.length ? ids.join(" ") : undefined;
}

export function Campo({ etiqueta, error, ayuda, ...resto }: BaseCampo & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <input id={id} aria-invalid={Boolean(error)} aria-describedby={describedBy(id, ayuda, error)} {...resto} />
      <Mensajes idAyuda={`${id}-ayuda`} idError={`${id}-error`} ayuda={ayuda} error={error} />
    </div>
  );
}

export function AreaTexto({ etiqueta, error, ayuda, ...resto }: BaseCampo & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <textarea id={id} rows={4} aria-invalid={Boolean(error)} aria-describedby={describedBy(id, ayuda, error)} {...resto} />
      <Mensajes idAyuda={`${id}-ayuda`} idError={`${id}-error`} ayuda={ayuda} error={error} />
    </div>
  );
}

export function Selector({
  etiqueta,
  error,
  ayuda,
  opciones,
  ...resto
}: BaseCampo & SelectHTMLAttributes<HTMLSelectElement> & { opciones: { valor: string; texto: string }[] }) {
  const id = useId();
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <select id={id} aria-invalid={Boolean(error)} aria-describedby={describedBy(id, ayuda, error)} {...resto}>
        {opciones.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.texto}
          </option>
        ))}
      </select>
      <Mensajes idAyuda={`${id}-ayuda`} idError={`${id}-error`} ayuda={ayuda} error={error} />
    </div>
  );
}

/** Interruptor accesible (`role="switch"`). */
export function Interruptor({
  etiqueta,
  activo,
  onCambio,
  detalle,
}: {
  etiqueta: string;
  activo: boolean;
  onCambio: (activo: boolean) => void;
  detalle?: ReactNode;
}) {
  const id = useId();
  return (
    <div className="interruptor">
      <button
        type="button"
        role="switch"
        aria-checked={activo}
        aria-labelledby={`${id}-etiqueta`}
        className={`interruptor__control ${activo ? "activo" : ""}`}
        onClick={() => onCambio(!activo)}
      >
        <span className="interruptor__perilla" />
      </button>
      <div>
        <span id={`${id}-etiqueta`} className="interruptor__etiqueta">
          {etiqueta}
        </span>
        {detalle && <div className="campo__ayuda">{detalle}</div>}
      </div>
    </div>
  );
}

/**
 * Diálogo modal con el elemento nativo `<dialog>`: atrapa el foco, cierra con Escape y devuelve el
 * foco al botón que lo abrió.
 */
export function Dialogo({
  abierto,
  titulo,
  onCerrar,
  children,
  acciones,
}: {
  abierto: boolean;
  titulo: string;
  onCerrar: () => void;
  children: ReactNode;
  acciones: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (abierto && !dialogo.open) dialogo.showModal();
    if (!abierto && dialogo.open) dialogo.close();
  }, [abierto]);
  return (
    <dialog ref={ref} className="dialogo" aria-labelledby={`${id}-titulo`} onClose={onCerrar}>
      <h2 id={`${id}-titulo`}>{titulo}</h2>
      <div className="pila">{children}</div>
      <div className="dialogo__acciones">{acciones}</div>
    </dialog>
  );
}
